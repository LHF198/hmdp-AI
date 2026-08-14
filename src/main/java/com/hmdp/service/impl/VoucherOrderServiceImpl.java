package com.hmdp.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hmdp.dto.OrderVO;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.enums.OrderStatusEnum;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SeckillConstants;
import com.hmdp.utils.UserHolder;

import cn.hutool.core.bean.BeanUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 秒杀下单服务实现类：Lua 脚本原子校验库存/一人一单/时间窗后投递 Redis Stream， daemon
 * 消费线程池异步落库（编程式事务），启动自动建消费者组，关闭时优雅停机
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private VoucherMapper voucherMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 查询当前登录用户的秒杀订单列表，按下单时间倒序返回
     */
    @Override
    public Result queryMyOrders() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.查询该用户的订单，按下单时间倒序
        List<VoucherOrder> orders = lambdaQuery()
                .eq(VoucherOrder::getUserId, userId)
                .orderByDesc(VoucherOrder::getCreateTime)
                .list();
        // 3.批量查询订单关联的代金券（一次 IN 查询，避免逐单 selectById 的 N+1）
        List<Long> voucherIds = orders.stream()
                .map(VoucherOrder::getVoucherId)
                .distinct()
                .toList();
        Map<Long, Voucher> voucherMap = voucherIds.isEmpty()
                ? Collections.emptyMap()
                : voucherMapper.selectBatchIds(voucherIds).stream()
                        .collect(Collectors.toMap(Voucher::getId, v -> v));
        // 4.组装订单展示对象，填充代金券标题与金额
        List<OrderVO> result = new ArrayList<>(orders.size());
        for (VoucherOrder order : orders) {
            OrderVO vo = new OrderVO();
            vo.setId(order.getId());
            vo.setVoucherId(order.getVoucherId());
            vo.setStatus(order.getStatus());
            vo.setCreateTime(order.getCreateTime());
            Voucher voucher = voucherMap.get(order.getVoucherId());
            if (voucher != null) {
                vo.setVoucherTitle(voucher.getTitle());
                vo.setPayValue(voucher.getPayValue());
            }
            result.add(vo);
        }
        return Result.ok(result);
    }

    /**
     * 模拟支付：校验本人 + 未支付状态后置为已支付（演示环境无真实支付通道）
     */
    @Override
    public Result payOrder(Long orderId) {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.查询订单并校验归属
        VoucherOrder order = getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return Result.fail("订单不存在");
        }
        // 3.仅未支付订单可支付
        if (order.getStatus() != OrderStatusEnum.UNPAID.getCode()) {
            return Result.fail("当前订单状态不可支付");
        }
        // 4.更新状态为已支付
        boolean isSuccess = lambdaUpdate()
                .set(VoucherOrder::getStatus, OrderStatusEnum.PAID.getCode())
                .eq(VoucherOrder::getId, orderId)
                .eq(VoucherOrder::getStatus, OrderStatusEnum.UNPAID.getCode())
                .update();
        if (!isSuccess) {
            return Result.fail("支付失败，请重试");
        }
        return Result.ok();
    }

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * pending 消息最大重试次数，超过后移入死信队列，防止单条坏消息永久阻塞消费流水线
     */
    private static final int MAX_PENDING_RETRY = 3;
    /**
     * 记录每条 pending 消息的处理失败次数（key: Redis Stream 消息 id）
     */
    private final Map<String, Integer> pendingRetryCount = new ConcurrentHashMap<>();

    /**
     * 秒杀订单异步处理线程池。 使用 daemon 线程：应用关闭（Spring 销毁 Redis 连接工厂）后， 即使消费线程仍处于
     * while(true) 循环中，也不会阻止 JVM 退出， 避免 "LettuceConnectionFactory was destroyed"
     * 错误持续刷屏和僵尸进程残留。
     */
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "seckill-order-handler");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    private void init() {
        // 确保 Stream 和消费者组存在（Redis 重启后自动创建）
        ensureStreamGroup();
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    @PreDestroy
    private void destroy() {
        // 应用关闭时中断消费线程，配合 daemon 线程确保 JVM 正常退出
        SECKILL_ORDER_EXECUTOR.shutdownNow();
    }

    private void ensureStreamGroup() {
        try {
            // 检查消费者组是否存在
            StreamInfo.XInfoGroups groups = stringRedisTemplate.opsForStream().groups(SeckillConstants.STREAM_ORDERS);
            boolean groupExists = groups.stream().anyMatch(g -> SeckillConstants.STREAM_GROUP.equals(g.groupName()));
            if (!groupExists) {
                stringRedisTemplate.opsForStream().createGroup(SeckillConstants.STREAM_ORDERS, SeckillConstants.STREAM_GROUP);
                log.info("创建消费者组 {} 成功", SeckillConstants.STREAM_GROUP);
            }
        } catch (Exception e) {
            // Stream 不存在，创建 Stream 和消费者组
            try {
                stringRedisTemplate.opsForStream().createGroup(SeckillConstants.STREAM_ORDERS, SeckillConstants.STREAM_GROUP);
                log.info("创建 Stream 和消费者组 {} 成功", SeckillConstants.STREAM_GROUP);
            } catch (Exception ex) {
                log.warn("初始化消费者组失败，将在后台线程中重试: {}", ex.getMessage());
            }
        }
    }

    private class VoucherOrderHandler implements Runnable {

        @Override
        public void run() {
            // 应用关闭（shutdownNow）时中断标志被设置，循环退出
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 10 BLOCK 2000 STREAMS s1 >
                    // 批量拉取减少 Redis 往返，提升消费吞吐（单线程下从每条 1 次往返降为每 10 条 1 次）
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from(SeckillConstants.STREAM_GROUP, SeckillConstants.STREAM_CONSUMER),
                            StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                            StreamOffset.create(SeckillConstants.STREAM_ORDERS, ReadOffset.lastConsumed())
                    );
                    // 2.判断订单信息是否为空
                    if (list == null || list.isEmpty()) {
                        // 如果为null，说明没有消息，继续下一次循环
                        continue;
                    }
                    // 3.逐条处理本批消息：任一失败会进入 catch -> handlePendingList 兜底重试
                    for (MapRecord<String, Object, Object> record : list) {
                        // 解析数据
                        Map<Object, Object> value = record.getValue();
                        VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                        // 3.1.创建订单
                        createVoucherOrder(voucherOrder);
                        // 3.2.确认消息 XACK
                        stringRedisTemplate.opsForStream().acknowledge(SeckillConstants.STREAM_ORDERS, SeckillConstants.STREAM_GROUP, record.getId());
                    }
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    handlePendingList();
                }
            }
        }

        private void handlePendingList() {
            // 应用关闭（shutdownNow）时中断标志被设置，循环退出
            while (!Thread.currentThread().isInterrupted()) {
                MapRecord<String, Object, Object> record = null;
                try {
                    // 1.获取pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from(SeckillConstants.STREAM_GROUP, SeckillConstants.STREAM_CONSUMER),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(SeckillConstants.STREAM_ORDERS, ReadOffset.from("0"))
                    );
                    // 2.判断订单信息是否为空
                    if (list == null || list.isEmpty()) {
                        // 如果为null，说明没有异常消息，结束循环
                        break;
                    }
                    // 解析数据
                    record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 3.创建订单
                    createVoucherOrder(voucherOrder);
                    // 4.确认消息 XACK
                    stringRedisTemplate.opsForStream().acknowledge(SeckillConstants.STREAM_ORDERS, SeckillConstants.STREAM_GROUP, record.getId());
                    // 处理成功，清除该消息的重试计数
                    pendingRetryCount.remove(record.getId().getValue());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    // 单条坏消息连续失败：超过上限移入死信队列并确认，避免无限重试阻塞整个消费流水线
                    if (record != null && moveToDlq(record)) {
                        continue;
                    }
                    // 等待 1 秒后重试，避免在 Redis Stream 不存在（NOGROUP）时疯狂刷日志
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        /**
         * 累计消息失败次数，超过上限后移入死信队列并确认原消息
         *
         * @return true-已移入死信队列；false-未达上限或移入失败，需继续重试
         */
        private boolean moveToDlq(MapRecord<String, Object, Object> record) {
            String recordId = record.getId().getValue();
            int count = pendingRetryCount.merge(recordId, 1, Integer::sum);
            if (count < MAX_PENDING_RETRY) {
                return false;
            }
            try {
                // 1.坏消息写入死信队列落盘
                stringRedisTemplate.opsForStream().add(SeckillConstants.STREAM_ORDERS_DLQ, record.getValue());
                // 2.确认原消息，避免再次进入 pending
                stringRedisTemplate.opsForStream().acknowledge(SeckillConstants.STREAM_ORDERS, SeckillConstants.STREAM_GROUP, record.getId());
                log.error("订单消息处理失败 {} 次，已移入死信队列 {}: id={}", count, SeckillConstants.STREAM_ORDERS_DLQ, recordId);
                return true;
            } catch (Exception ex) {
                log.error("订单消息移入死信队列失败，稍后重试: id={}", recordId, ex);
                return false;
            }
        }
    }

    private void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        // 创建锁对象
        RLock redisLock = redissonClient.getLock(RedisConstants.LOCK_ORDER_KEY + userId);
        // 尝试获取锁（显式等待超时，避免单条消息无限阻塞消费流水线）
        boolean isLock;
        try {
            isLock = redisLock.tryLock(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断, userId={}, voucherId={}", userId, voucherId, e);
            // 中断（应用关闭）时抛异常：消息不 ACK 留在 pending，重启后重新投递，保证不丢单
            throw new IllegalStateException("获取分布式锁被中断，消息等待重新投递", e);
        }
        // 判断
        if (!isLock) {
            // 获取锁失败（锁被其他请求短暂持有）：抛异常使消息进入 pending 重试，
            // 超过上限移入死信队列告警，而不是静默 ACK 丢弃导致订单丢失
            log.error("获取分布式锁失败，消息进入重试: userId={}, voucherId={}", userId, voucherId);
            throw new IllegalStateException("获取分布式锁失败，等待重试: userId=" + userId + ", voucherId=" + voucherId);
        }

        try {
            // 扣库存 + 建订单放入同一事务：任一失败整体回滚，避免“库存已扣、订单未建”
            Boolean processed = transactionTemplate.execute(status -> {
                // 5.1.查询订单
                Long count = lambdaQuery().eq(VoucherOrder::getUserId, userId).eq(VoucherOrder::getVoucherId, voucherId).count();
                // 5.2.判断是否存在
                if (count > 0) {
                    // 用户已经购买过了（业务失败不抛异常，消息仍被确认丢弃）
                    log.error("不允许重复下单！userId={}, voucherId={}", userId, voucherId);
                    return false;
                }

                // 6.扣减库存
                boolean success = seckillVoucherService.lambdaUpdate()
                        .setSql("stock = stock - 1") // set stock = stock - 1
                        .eq(SeckillVoucher::getVoucherId, voucherId).gt(SeckillVoucher::getStock, 0) // where id = ? and stock > 0
                        .update();
                if (!success) {
                    // 扣减失败（库存不足，业务失败不抛异常，消息确认丢弃）
                    log.error("库存不足！voucherId={}", voucherId);
                    return false;
                }

                // 7.创建订单
                save(voucherOrder);
                return true;
            });
            if (Boolean.FALSE.equals(processed)) {
                log.warn("订单业务校验未通过，消息确认丢弃: voucherId={}, userId={}", voucherId, userId);
            }
        } finally {
            // 释放锁（仅当前线程持有才释放，防止释放他人锁）
            if (redisLock.isHeldByCurrentThread()) {
                redisLock.unlock();
            }
        }
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        int r = result.intValue();
        // 2.判断结果是否为0（返回码语义见 SeckillConstants，与 seckill.lua return 值一一对应）
        if (r == SeckillConstants.SECKILL_STOCK_NOT_ENOUGH) {
            return Result.fail("库存不足");
        }
        if (r == SeckillConstants.SECKILL_REPEAT_ORDER) {
            return Result.fail("不能重复下单");
        }
        if (r == SeckillConstants.SECKILL_NOT_BEGIN) {
            return Result.fail("秒杀尚未开始");
        }
        if (r == SeckillConstants.SECKILL_ENDED) {
            return Result.fail("秒杀已经结束");
        }
        // 3.返回订单id
        return Result.ok(orderId);
    }
}
