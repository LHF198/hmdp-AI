package com.hmdp.ai.tool.vo;

/**
 * 店铺信息（工具调用返回给模型的轻量视图）
 */
public record ShopVO(
        Long id,
        String name,
        String typeName,
        String area,
        String address,
        Integer avgPrice,
        Integer sold,
        Integer comments,
        Double score,
        String openHours
) {
}
