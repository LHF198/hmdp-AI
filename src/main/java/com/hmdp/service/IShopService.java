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
}
