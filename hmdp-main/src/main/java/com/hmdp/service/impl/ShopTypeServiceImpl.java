package com.hmdp.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.RedisConstants;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result getTypeList(){
        String typeKey = RedisConstants.CACHE_TYPE_KEY;
        //1.获取缓存数据
        Long size = stringRedisTemplate.opsForList().size(typeKey);
        //2.缓存数据存在
        if(size != null && size != 0){
            // 历史写入的缓存可能未设置过期时间（永久驻留），命中时补齐 TTL，避免类型变更后长期脏读
            Long ttl = stringRedisTemplate.getExpire(typeKey, TimeUnit.HOURS);
            if (ttl != null && ttl < 0) {
                stringRedisTemplate.expire(typeKey, RedisConstants.CACHE_TYPE_TTL, TimeUnit.HOURS);
            }
            List<String> typeList = stringRedisTemplate.opsForList().range(typeKey, 0, -1);
            List<ShopType> shopTypes = new ArrayList<>();
            for (String type : typeList) {
                shopTypes.add(JSONUtil.toBean(type, ShopType.class));
            }
            return Result.ok(shopTypes);
        }
        //3.缓存数据不存在
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        if(shopTypes == null){
            //4.不存在，返回错误
            return Result.fail("店铺类型不存在");
        }
        //5.存在，写入缓存
        List<String> typeList = new ArrayList<>();
        for (ShopType shopType : shopTypes) {
            typeList.add(JSONUtil.toJsonStr(shopType));
        }
        stringRedisTemplate.opsForList().rightPushAll(typeKey, typeList);
        //6.设置过期时间，类型数据变更后最坏 24 小时自动失效重建
        stringRedisTemplate.expire(typeKey, RedisConstants.CACHE_TYPE_TTL, TimeUnit.HOURS);
        return Result.ok(shopTypes);
    }

}
