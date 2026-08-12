package com.hmdp.controller;


import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.SystemConstants;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 */
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    public IShopService shopService;

    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        return shopService.queryById(id);
    }

    /**
     * 新增商铺信息
     * @param shop 商铺数据
     * @return 商铺id
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        // 写入数据库
        shopService.save(shop);
        // 返回店铺id
        return Result.ok(shop.getId());
    }

    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 无
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        // 写入数据库
        return shopService.update(shop);
    }

    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId 商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "isAsc", required = false) Boolean isAsc
    ) {
       return shopService.queryShopByType(typeId, current, x, y, city, sortBy, isAsc);
    }

    /**
     * 全部城市列表（供主页地区选择器使用，数据驱动）
     * @return 城市名列表
     */
    @GetMapping("/cities")
    public Result queryCities() {
        // 常用城市保底，避免演示数据仅覆盖单城时下拉只有一项；与店铺数据取并集去重
        List<String> cities = new java.util.ArrayList<>(java.util.Arrays.asList(
                "杭州", "上海", "北京", "广州", "深圳", "成都", "南京", "武汉"));
        shopService.query()
                .select("DISTINCT city")
                .list()
                .stream()
                .map(Shop::getCity)
                .filter(StrUtil::isNotBlank)
                .forEach(c -> {
                    if (!cities.contains(c)) {
                        cities.add(c);
                    }
                });
        return Result.ok(cities);
    }

    /**
     * 地图页：返回全部商铺（含经纬度），供前端按类型过滤并投影渲染
     * @return 全部商铺列表
     */
    @GetMapping("/map/list")
    public Result queryAllForMap() {
        List<Shop> shops = shopService.query().select("id", "name", "type_id", "city", "area", "address", "x", "y", "avg_price", "sold", "comments", "score").list();
        return Result.ok(shops);
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     * @param name 商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "isAsc", required = false) Boolean isAsc
    ) {
        // 仅允许白名单排序字段，避免 SQL 注入
        String column = null;
        if ("comments".equals(sortBy)) {
            column = "comments";
        } else if ("score".equals(sortBy)) {
            column = "score";
        }
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .eq(StrUtil.isNotBlank(city), "city", city)
                .orderBy(column != null, !Boolean.FALSE.equals(isAsc), column)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }
}
