package com.hmdp.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;

/**
 * <p>
 * 店铺服务接口：按 id 查询店铺（带缓存）、更新店铺（先改库再删缓存）、 按类型分页查询（附近/距离排序）
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result update(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y, String city, String sortBy, Boolean isAsc);

    /**
     * 全部城市列表（主页地区选择器用，Redis 缓存 24h：常用城市保底 + DB 去重并集）
     */
    Result queryCities();

    /**
     * 地图页：返回全部商铺（含经纬度），供前端按类型过滤并投影渲染
     */
    Result queryAllForMap();

    /**
     * 根据商铺名称关键字分页查询商铺信息（排序字段白名单校验，防 SQL 注入）
     */
    Result queryShopByName(String name, Integer current, String city, String sortBy, Boolean isAsc);
}
