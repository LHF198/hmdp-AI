package com.hmdp.enums;

/**
 * 支付方式枚举：与 tb_voucher_order.pay_type 对应（1 余额支付、2 支付宝、3 微信）。
 *
 * <p>实体字段保留 Integer 存储（避免引入枚举映射与序列化兼容问题），本枚举仅作状态码字典。
 */
public enum PayTypeEnum {

    BALANCE(1, "余额支付"),
    ALIPAY(2, "支付宝"),
    WECHAT(3, "微信");

    private final int code;

    private final String desc;

    PayTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
