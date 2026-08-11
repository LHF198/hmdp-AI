package com.hmdp.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;

import jakarta.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }

    /**
     * 查询当前登录用户的秒杀订单列表
     */
    @GetMapping("list")
    public Result queryMyOrders() {
        return voucherOrderService.queryMyOrders();
    }

    /**
     * 模拟支付（演示环境），订单状态 未支付 → 已支付
     */
    @PutMapping("pay/{id}")
    public Result payOrder(@PathVariable("id") Long orderId) {
        return voucherOrderService.payOrder(orderId);
    }
}
