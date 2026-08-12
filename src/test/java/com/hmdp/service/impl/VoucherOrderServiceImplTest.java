package com.hmdp.service.impl;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;

/**
 * VoucherOrderServiceImpl 单元测试：覆盖秒杀 Lua 脚本各返回码分支与订单支付校验。 通过 Mockito 隔离 Redis /
 * Redisson / 数据库依赖，不启动消费线程（@PostConstruct 不触发）。
 */
class VoucherOrderServiceImplTest {

    private VoucherOrderServiceImpl service;
    private StringRedisTemplate stringRedisTemplate;
    private RedisIdWorker redisIdWorker;
    private UpdateChainWrapper<VoucherOrder> updateChain;

    @BeforeEach
    void setUp() {
        service = spy(new VoucherOrderServiceImpl());
        stringRedisTemplate = mock(StringRedisTemplate.class);
        redisIdWorker = mock(RedisIdWorker.class);
        updateChain = mock(UpdateChainWrapper.class);
        when(updateChain.set(anyString(), any())).thenReturn(updateChain);
        when(updateChain.eq(anyString(), any())).thenReturn(updateChain);
        when(updateChain.update()).thenReturn(true);
        doReturn(updateChain).when(service).update();
        ReflectionTestUtils.setField(service, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(service, "redisIdWorker", redisIdWorker);

        UserDTO user = new UserDTO();
        user.setId(1L);
        UserHolder.saveUser(user);
    }

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    private void mockLuaResult(Long result) {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(result);
    }

    // ==================== seckillVoucher：Lua 脚本返回码分支 ====================
    @Test
    void seckillVoucher_校验通过时返回订单ID() {
        mockLuaResult(0L);
        when(redisIdWorker.nextId("order")).thenReturn(123L);

        Result r = service.seckillVoucher(100L);

        assertTrue(r.getSuccess());
        assertEquals(123L, r.getData());
    }

    @Test
    void seckillVoucher_库存不足时失败() {
        mockLuaResult(1L);

        Result r = service.seckillVoucher(100L);

        assertEquals("库存不足", r.getErrorMsg());
    }

    @Test
    void seckillVoucher_重复下单时失败() {
        mockLuaResult(2L);

        Result r = service.seckillVoucher(100L);

        assertEquals("不能重复下单", r.getErrorMsg());
    }

    @Test
    void seckillVoucher_秒杀未开始时失败() {
        mockLuaResult(3L);

        Result r = service.seckillVoucher(100L);

        assertEquals("秒杀尚未开始", r.getErrorMsg());
    }

    @Test
    void seckillVoucher_秒杀已结束时失败() {
        mockLuaResult(4L);

        Result r = service.seckillVoucher(100L);

        assertEquals("秒杀已经结束", r.getErrorMsg());
    }

    // ==================== payOrder：订单归属与状态校验 ====================
    @Test
    void payOrder_非本人订单时拒绝() {
        VoucherOrder order = new VoucherOrder();
        order.setId(10L);
        order.setUserId(999L);
        order.setStatus(1);
        doReturn(order).when(service).getById(10L);

        Result r = service.payOrder(10L);

        assertEquals("订单不存在", r.getErrorMsg());
    }

    @Test
    void payOrder_已支付订单时拒绝() {
        VoucherOrder order = new VoucherOrder();
        order.setId(10L);
        order.setUserId(1L);
        order.setStatus(2);
        doReturn(order).when(service).getById(10L);

        Result r = service.payOrder(10L);

        assertEquals("当前订单状态不可支付", r.getErrorMsg());
    }

    @Test
    void payOrder_未支付且本人订单时支付成功() {
        VoucherOrder order = new VoucherOrder();
        order.setId(10L);
        order.setUserId(1L);
        order.setStatus(1);
        doReturn(order).when(service).getById(10L);

        Result r = service.payOrder(10L);

        assertTrue(r.getSuccess());
        // 更新条件必须带上 status=1，防止并发下重复支付
        verify(updateChain).eq("status", 1);
    }

    @Test
    void payOrder_更新失败时返回失败() {
        VoucherOrder order = new VoucherOrder();
        order.setId(10L);
        order.setUserId(1L);
        order.setStatus(1);
        doReturn(order).when(service).getById(10L);
        when(updateChain.update()).thenReturn(false);

        Result r = service.payOrder(10L);

        assertEquals("支付失败，请重试", r.getErrorMsg());
    }
}
