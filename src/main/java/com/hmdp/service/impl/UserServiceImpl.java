package com.hmdp.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_TTL;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
import static com.hmdp.utils.RedisConstants.USER_SIGN_KEY;
import com.hmdp.utils.RegexUtils;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;
import com.hmdp.utils.UserHolder;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 用户服务实现类：发送短信验证码（存 Redis）、手机号+验证码/密码登录、 基于 Redis 的 token 分布式会话（不用
 * Session，支持集群部署）
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 演示环境无短信通道时是否将验证码回显给前端（app.demo.echo-code，仅开发 profile 开启；生产禁止）
     */
    @Value("${app.demo.echo-code:false}")
    private boolean echoCode;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3.发送冷却：验证码 TTL 内存在未消费验证码则拒绝重发（防短信轰炸 + 防验证码枚举）
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(LOGIN_CODE_KEY + phone))) {
            return Result.fail("验证码已发送，请稍后再试");
        }
        // 4.生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 5.保存验证码到 Redis（TTL 过期自动失效）
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 6.发送验证码（真实环境接入短信通道；演示环境按配置决定是否回显）
        log.debug("发送短信验证码成功，验证码：{}", code);
        if (echoCode) {
            // 仅演示环境回显给前端展示
            return Result.ok(code);
        }
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3.从redis获取验证码并校验
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            // 不一致，报错
            return Result.fail("验证码错误");
        }
        // 验证码一次性使用：校验通过立即作废，防止 TTL 内被复用
        stringRedisTemplate.delete(LOGIN_CODE_KEY + phone);

        // 4.一致，根据手机号查询用户 select * from tb_user where phone = ?
        User user = lambdaQuery().eq(User::getPhone, phone).one();

        // 5.判断用户是否存在
        if (user == null) {
            // 6.不存在，创建新用户并保存
            user = createUserWithPhone(phone);
        }

        // 7.保存用户信息到 redis中
        // 7.1.随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        // 7.2.将User对象转为HashMap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        // 7.3.存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 7.4.设置token有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 8.返回token
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.写入Redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:5:202203 GET u14 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            // 没有任何签到结果
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        // 6.循环遍历
        int count = 0;
        while (true) {
            // 6.1.让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
            if ((num & 1) == 0) {
                // 如果为0，说明未签到，结束
                break;
            } else {
                // 如果不为0，说明已签到，计数器+1
                count++;
            }
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }
        return Result.ok(count);
    }

    private User createUserWithPhone(String phone) {
        // 1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 2.保存用户（并发注册同一手机号时唯一索引兜底，冲突后重查返回已有账号，避免 500）
        try {
            save(user);
        } catch (DuplicateKeyException e) {
            log.info("手机号已注册，复用已有账号: {}", phone);
            return lambdaQuery().eq(User::getPhone, phone).one();
        }
        // 3.同步初始化个人资料记录（保证编辑资料时走 update 而非 insert）
        UserInfo info = new UserInfo();
        info.setUserId(user.getId());
        info.setCity("杭州");
        info.setIntroduce("");
        info.setGender(false);
        userInfoService.save(info);
        return user;
    }

    @Override
    public void updateProfile(Long userId, String nickName, String icon, String token) {
        // 1.修改昵称（tb_user），并同步 Redis 登录态，保证 /user/me 立即生效
        if (nickName != null && StrUtil.isNotBlank(nickName)) {
            lambdaUpdate().set(User::getNickName, nickName).eq(User::getId, userId).update();
            if (StrUtil.isNotBlank(token)) {
                stringRedisTemplate.opsForHash().put(LOGIN_USER_KEY + token, "nickName", nickName);
            }
        }
        // 2.修改头像（tb_user），并同步 Redis 登录态
        if (icon != null && StrUtil.isNotBlank(icon)) {
            lambdaUpdate().set(User::getIcon, icon).eq(User::getId, userId).update();
            if (StrUtil.isNotBlank(token)) {
                stringRedisTemplate.opsForHash().put(LOGIN_USER_KEY + token, "icon", icon);
            }
        }
    }
}
