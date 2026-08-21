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
import com.hmdp.utils.PasswordEncoder;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_TTL;
import static com.hmdp.utils.RedisConstants.LOGIN_FAIL_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_FAIL_TTL;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
import static com.hmdp.utils.RedisConstants.USER_SIGN_KEY;
import com.hmdp.utils.RegexPatterns;
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
        log.info("发送验证码成功: phone={}", phone);
        if (echoCode) {
            // 仅演示环境回显给前端展示
            return Result.ok(code);
        }
        return Result.ok();
    }

    /**
     * 密码登录连续失败次数上限，达到后锁定 {@link RedisConstants#LOGIN_FAIL_TTL} 分钟
     */
    private static final int MAX_PASSWORD_FAIL_TIMES = 5;

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3.双模式登录：验证码 / 密码二选一（都为空视为参数缺失）
        if (StrUtil.isNotBlank(loginForm.getCode())) {
            return loginByCode(phone, loginForm.getCode());
        }
        if (StrUtil.isNotBlank(loginForm.getPassword())) {
            return loginByPassword(phone, loginForm.getPassword());
        }
        return Result.fail("请输入验证码或密码");
    }

    /**
     * 验证码登录/注册：校验验证码（一次性使用），未注册手机号自动创建账号
     */
    private Result loginByCode(String phone, String code) {
        // 1.从redis获取验证码并校验
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if (cacheCode == null || !cacheCode.equals(code)) {
            // 不一致，报错
            return Result.fail("验证码错误");
        }
        // 验证码一次性使用：校验通过立即作废，防止 TTL 内被复用
        stringRedisTemplate.delete(LOGIN_CODE_KEY + phone);

        // 2.根据手机号查询用户 select * from tb_user where phone = ?
        User user = lambdaQuery().eq(User::getPhone, phone).one();
        // 3.判断用户是否存在
        if (user == null) {
            // 不存在，创建新用户并保存
            user = createUserWithPhone(phone);
        }
        // 4.签发登录态
        return doLogin(user, "code");
    }

    /**
     * 密码登录：校验账号存在、已设置密码、密码正确，连续失败计数锁定防暴力破解
     */
    private Result loginByPassword(String phone, String password) {
        // 1.连续失败锁定检查
        String failKey = LOGIN_FAIL_KEY + phone;
        String failCountStr = stringRedisTemplate.opsForValue().get(failKey);
        if (failCountStr != null) {
            int failCount = 0;
            try {
                failCount = Integer.parseInt(failCountStr);
            } catch (NumberFormatException e) {
                // 计数数据异常（如外部写入）：按 0 处理，不影响登录可用性
                log.warn("密码失败计数格式异常，忽略: key={}, value={}", failKey, failCountStr);
            }
            if (failCount >= MAX_PASSWORD_FAIL_TIMES) {
                return Result.fail("密码错误次数过多，请 " + LOGIN_FAIL_TTL + " 分钟后再试，或使用验证码登录");
            }
        }
        // 2.查询用户
        User user = lambdaQuery().eq(User::getPhone, phone).one();
        if (user == null) {
            return Result.fail("该手机号未注册，请使用验证码登录");
        }
        if (StrUtil.isBlank(user.getPassword())) {
            return Result.fail("该账号尚未设置密码，请先用验证码登录后在「我的-修改密码」中设置");
        }
        // 3.校验密码
        if (!PasswordEncoder.matches(user.getPassword(), password)) {
            // 4.失败计数 +1，首次失败时设置窗口过期时间
            Long count = stringRedisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(failKey, LOGIN_FAIL_TTL, TimeUnit.MINUTES);
            }
            int remain = Math.max(0, MAX_PASSWORD_FAIL_TIMES - (count == null ? 1 : count.intValue()));
            log.warn("密码登录失败: phone={}, failCount={}, remain={}", phone, count, remain);
            return Result.fail("密码错误" + (remain > 0 ? "，还可尝试 " + remain + " 次" : "，请稍后再试或使用验证码登录"));
        }
        // 5.登录成功：清除失败计数并签发登录态
        stringRedisTemplate.delete(failKey);
        return doLogin(user, "password");
    }

    /**
     * 签发登录态：生成 token，用户信息写入 Redis（hash），返回 token
     */
    private Result doLogin(User user, String loginMethod) {
        // 1.随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        // 2.将User对象转为HashMap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        // 3.存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 4.设置token有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 5.记录登录成功日志
        log.info("用户登录成功: userId={}, phone={}, method={}", user.getId(), user.getPhone(), loginMethod);
        // 6.返回token
        return Result.ok(token);
    }

    @Override
    public Result setPassword(String oldPassword, String newPassword) {
        // 1.校验新密码格式
        if (StrUtil.isBlank(newPassword) || !newPassword.matches(RegexPatterns.PASSWORD_REGEX)) {
            return Result.fail("密码格式不正确（4~32位字母、数字或下划线）");
        }
        // 2.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        User user = getById(userId);
        if (user == null) {
            log.warn("修改密码时用户不存在: userId={}", userId);
            return Result.fail("用户不存在");
        }
        // 3.已有密码时必须校验原密码
        if (StrUtil.isNotBlank(user.getPassword())) {
            if (StrUtil.isBlank(oldPassword) || !PasswordEncoder.matches(user.getPassword(), oldPassword)) {
                log.warn("修改密码时原密码错误: userId={}", userId);
                return Result.fail("原密码错误");
            }
        }
        // 4.更新密码（MD5 + 随机盐，见 PasswordEncoder）
        boolean isSuccess = lambdaUpdate()
                .set(User::getPassword, PasswordEncoder.encode(newPassword))
                .eq(User::getId, userId)
                .update();
        return isSuccess ? Result.ok() : Result.fail("密码修改失败，请重试");
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
