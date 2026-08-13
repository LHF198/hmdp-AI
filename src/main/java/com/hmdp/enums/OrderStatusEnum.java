package com.hmdp.enums;

/**
 * 订单状态枚举：与 tb_voucher_order.status 对应
 * （1 未支付、2 已支付、3 已核销、4 已取消、5 退款中、6 已退款）。
 *
 * <p>实体字段保留 Integer 存储（避免引入枚举映射与序列化兼容问题），本枚举仅作状态码字典，
 * 业务判断统一使用 {@code OrderStatusEnum.XXX.getCode()}，禁止直接写魔法数字。
 */
public enum OrderStatusEnum {

    UNPAID(1, "未支付"),
    PAID(2, "已支付"),
    USED(3, "已核销"),
    CANCELLED(4, "已取消"),
    REFUNDING(5, "退款中"),
    REFUNDED(6, "已退款");

    private final int code;

    private final String desc;

    OrderStatusEnum(int code, String desc) {
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
