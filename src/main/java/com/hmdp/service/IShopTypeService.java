package com.hmdp.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;

/**
 * <p>
 * 店铺类型服务接口：查询全部店铺分类列表（带缓存）
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopTypeService extends IService<ShopType> {

    Result getTypeList();

}
