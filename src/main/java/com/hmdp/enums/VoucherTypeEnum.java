package com.hmdp.enums;

/**
 * 优惠券类型枚举：与 tb_voucher.type 对应（0 普通券、1 秒杀券）。
 *
 * <p>秒杀券在 tb_seckill_voucher 表中持有额外秒杀字段（库存/起止时间），
 * 本枚举仅作类型码字典，业务判断统一使用 {@code VoucherTypeEnum.XXX.getCode()}。
 */
public enum VoucherTypeEnum {

    NORMAL(0, "普通券"),
    SECKILL(1, "秒杀券");

    private final int code;

    private final String desc;

    VoucherTypeEnum(int code, String desc) {
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
