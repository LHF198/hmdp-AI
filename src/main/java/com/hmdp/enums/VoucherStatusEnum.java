package com.hmdp.enums;

/**
 * 优惠券状态枚举：与 tb_voucher.status 对应（1 上架、2 下架、3 过期）。
 *
 * <p>实体字段保留 Integer 存储（避免引入枚举映射与序列化兼容问题），本枚举仅作状态码字典。
 */
public enum VoucherStatusEnum {

    ON_SALE(1, "上架"),
    OFF_SALE(2, "下架"),
    EXPIRED(3, "过期");

    private final int code;

    private final String desc;

    VoucherStatusEnum(int code, String desc) {
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
