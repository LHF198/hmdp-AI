package com.hmdp.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;
import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;
import com.hmdp.utils.SystemConstants;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 店铺服务实现类：店铺缓存查询（互斥锁防击穿，可切换穿透/逻辑过期方案）、
 * 写库时先删缓存保证一致性、启动时初始化店铺 GEO 坐标
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    /**
     * 启动时把全部店铺经纬度按类型写入 Redis GEO（shop:geo:{typeId}）， 供店铺列表的附近/距离排序使用； 若 Redis
     * 数据被清空，queryShopByType 中的 GEO 空结果会回退数据库查询兜底
     */
    @PostConstruct
    public void initShopGeo() {
        try {
            List<Shop> shops = query().list();
            for (Shop shop : shops) {
                if (shop.getX() != null && shop.getY() != null) {
                    stringRedisTemplate.opsForGeo().add(
                            SHOP_GEO_KEY + shop.getTypeId(),
                            new Point(shop.getX(), shop.getY()),
                            shop.getId().toString());
                }
            }
            log.info("初始化店铺GEO缓存完成，共 {} 家店铺", shops.size());
        } catch (Exception e) {
            log.error("初始化店铺GEO缓存失败", e);
        }
    }

    @Override
    public Result queryById(Long id) {
        // 互斥锁解决缓存击穿（锁为 UUID+Lua 校验释放；重试 20 次后降级直查，避免无限自旋）
        Shop shop = cacheClient
                .queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 其他可选方案：缓存穿透用 queryWithPassThrough（空值占位）；
        // 追求读不阻塞用 queryWithLogicalExpire（逻辑过期 + 异步重建）
        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        // 7.返回
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除缓存（失败重试3次，避免旧数据长期驻留）
        deleteCacheWithRetry(CACHE_SHOP_KEY + id);
        // 3.坐标变更时同步更新 GEO 缓存，保证附近/距离排序与 DB 一致
        if (shop.getX() != null && shop.getY() != null && shop.getTypeId() != null) {
            stringRedisTemplate.opsForGeo().add(SHOP_GEO_KEY + shop.getTypeId(),
                    new Point(shop.getX(), shop.getY()), id.toString());
        }
        return Result.ok();
    }

    /**
     * 删除缓存，失败重试3次（100ms/200ms/300ms 退避），仍失败仅记录日志不阻塞主流程
     */
    private void deleteCacheWithRetry(String key) {
        for (int i = 1; i <= 3; i++) {
            try {
                stringRedisTemplate.delete(key);
                return;
            } catch (Exception e) {
                log.error("删除缓存失败，第 {} 次重试: key={}", i, key, e);
                try {
                    Thread.sleep(100L * i);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.error("删除缓存失败，已重试 3 次仍失败: key={}", key);
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y, String city, String sortBy, Boolean isAsc) {
        // 1.判断是否需要根据坐标查询；人气/评分排序不走GEO，直接数据库排序
        boolean dbSort = "comments".equals(sortBy) || "score".equals(sortBy);
        if (x == null || y == null || dbSort) {
            // 不需要坐标查询，按数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .eq(StrUtil.isNotBlank(city), "city", city)
                    .orderBy(dbSort, !Boolean.FALSE.equals(isAsc), sortBy)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }

        // 2.计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // 3.查询redis、按照距离排序、分页。结果：shopId、distance
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo() // GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );
        // 4.解析出id
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.isEmpty()) {
            // 4.0.GEO缓存缺失/被清空时回退数据库查询，避免店铺列表空白（按类型+城市分页）
            Page<Shop> fallback = query()
                    .eq("type_id", typeId)
                    .eq(StrUtil.isNotBlank(city), "city", city)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(fallback.getRecords());
        }
        if (list.size() <= from) {
            // 没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
        // 4.1.截取 from ~ end的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            // 4.2.获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            // 4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        // 4.4.距离倒序：反转id顺序（GEO默认按距离升序）
        if (Boolean.FALSE.equals(isAsc)) {
            Collections.reverse(ids);
        }
        // 5.根据id查询Shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).eq(StrUtil.isNotBlank(city), "city", city).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        // 6.返回
        return Result.ok(shops);
    }
}
