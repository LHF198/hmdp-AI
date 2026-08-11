package com.hmdp.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀订单展示对象，包含代金券信息
 */
@Data
public class OrderVO {

    /**
     * 订单id
     */
    private Long id;

    /**
     * 购买的代金券id
     */
    private Long voucherId;

    /**
     * 代金券标题
     */
    private String voucherTitle;

    /**
     * 支付金额（单位：分）
     */
    private Long payValue;

    /**
     * 订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款
     */
    private Integer status;

    /**
     * 下单时间
     */
    private LocalDateTime createTime;
}
