package com.hmdp.service.impl;

import java.time.ZoneId;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import static com.hmdp.utils.RedisConstants.SECKILL_BEGIN_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_END_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_ORDER_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 优惠券服务实现类：查询店铺优惠券列表、新增普通/秒杀券， 秒杀券同步写入 Redis（库存、开始/结束时间）供下单前置校验
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private VoucherOrderMapper voucherOrderMapper;

    /**
     * 启动时预热秒杀缓存，防止 Redis 重启/flushdb 后秒杀全部失效： - seckill:stock:{id} 缺失时用 DB
     * 剩余库存补写（否则 Lua 中 tonumber(nil)<=0 直接判“库存不足”） - seckill:order:{id}
     * 缺失时从订单表恢复已下单用户集合（防止 Redis 重启后同一用户重复下单） - seckill:begin:{id} /
     * seckill:end:{id} 无条件刷新为 DB 中的时间窗口（供 Lua 校验开抢时间）
     */
    @PostConstruct
    public void initSeckillCache() {
        try {
            List<SeckillVoucher> vouchers = seckillVoucherService.list();
            for (SeckillVoucher v : vouchers) {
                Long voucherId = v.getVoucherId();
                // 库存 key 不存在时补写（已存在不动，保留已扣减进度）
                if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(SECKILL_STOCK_KEY + voucherId))) {
                    stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucherId, String.valueOf(v.getStock()));
                }
                // 已下单用户集合缺失时从订单表恢复
                if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(SECKILL_ORDER_KEY + voucherId))) {
                    List<Object> userIds = voucherOrderMapper.selectObjs(
                            new LambdaQueryWrapper<VoucherOrder>().select(VoucherOrder::getUserId).eq(VoucherOrder::getVoucherId, voucherId));
                    if (!userIds.isEmpty()) {
                        String[] ids = userIds.stream().map(String::valueOf).toArray(String[]::new);
                        stringRedisTemplate.opsForSet().add(SECKILL_ORDER_KEY + voucherId, ids);
                    }
                }
                // 时间窗口无条件刷新，与 DB 保持同步
                stringRedisTemplate.opsForValue().set(SECKILL_BEGIN_KEY + voucherId,
                        String.valueOf(v.getBeginTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
                stringRedisTemplate.opsForValue().set(SECKILL_END_KEY + voucherId,
                        String.valueOf(v.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
            }
            log.info("预热秒杀缓存完成，共 {} 张秒杀券", vouchers.size());
        } catch (Exception e) {
            log.error("预热秒杀缓存失败", e);
        }
    }

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        // 保存秒杀库存到Redis中
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
        // 保存秒杀时间窗口到Redis中（供 Lua 脚本校验开抢时间）
        stringRedisTemplate.opsForValue().set(SECKILL_BEGIN_KEY + voucher.getId(),
                String.valueOf(voucher.getBeginTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        stringRedisTemplate.opsForValue().set(SECKILL_END_KEY + voucher.getId(),
                String.valueOf(voucher.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
    }
}
