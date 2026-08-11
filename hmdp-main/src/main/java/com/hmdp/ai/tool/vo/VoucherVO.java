package com.hmdp.ai.tool.vo;

/**
 * 优惠券信息（工具调用返回给模型的轻量视图，金额单位统一转为元）
 */
public record VoucherVO(
        Long id,
        Long shopId,
        String title,
        String subTitle,
        Double payValue,
        Double actualValue,
        Integer type,
        Integer status
) {
}
