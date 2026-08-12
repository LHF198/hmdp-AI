package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;

/**
 * <p>
 * 秒杀下单服务接口：创建秒杀订单（Lua 预校验 + 异步落库）、查询我的秒杀订单
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    Result queryMyOrders();

    /**
     * 模拟支付：仅本人可操作，订单状态 1（未支付）→ 2（已支付）
     */
    Result payOrder(Long orderId);
}
