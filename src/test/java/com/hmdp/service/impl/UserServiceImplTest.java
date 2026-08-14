package com.hmdp.service.impl;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.service.IUserInfoService;
import com.hmdp.utils.PasswordEncoder;
import com.hmdp.utils.UserHolder;

/**
 * UserServiceImpl 单元测试：覆盖双模式登录（验证码/密码）、密码登录失败锁定、
 * 设置/修改密码（原密码校验、格式校验）。通过 Mockito 隔离 Redis / 数据库依赖。
 */
class UserServiceImplTest {

    private UserServiceImpl userService;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private HashOperations<String, Object, Object> hashOperations;
    private LambdaQueryChainWrapper<User> queryChain;
    private LambdaUpdateChainWrapper<User> updateChain;

    private static final String PHONE = "13686869696";
    private static final String FAIL_KEY = "login:fail:" + PHONE;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        userService = spy(new UserServiceImpl());
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        hashOperations = mock(HashOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        queryChain = mock(LambdaQueryChainWrapper.class);
        when(queryChain.eq(any(), any())).thenReturn(queryChain);
        when(queryChain.one()).thenReturn(null); // 默认无此用户
        doReturn(queryChain).when(userService).lambdaQuery();

        updateChain = mock(LambdaUpdateChainWrapper.class);
        when(updateChain.set(any(), any())).thenReturn(updateChain);
        when(updateChain.eq(any(), any())).thenReturn(updateChain);
        when(updateChain.update()).thenReturn(true);
        doReturn(updateChain).when(userService).lambdaUpdate();

        IUserInfoService userInfoService = mock(IUserInfoService.class);
        ReflectionTestUtils.setField(userService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(userService, "userInfoService", userInfoService);
    }

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    private User newUser(Long id, String phone, String password) {
        User user = new User();
        user.setId(id);
        user.setPhone(phone);
        user.setPassword(password == null ? "" : password);
        user.setNickName("测试用户");
        return user;
    }

    private LoginFormDTO form(String code, String password) {
        LoginFormDTO form = new LoginFormDTO();
        form.setPhone(PHONE);
        form.setCode(code);
        form.setPassword(password);
        return form;
    }

    // ==================== login：参数校验与双模式分发 ====================

    @Test
    void login_验证码与密码都为空时提示() {
        Result r = userService.login(form(null, null), null);

        assertEquals("请输入验证码或密码", r.getErrorMsg());
    }

    @Test
    void login_验证码登录成功且用户已注册() {
        when(valueOperations.get("login:code:" + PHONE)).thenReturn("123456");
        when(queryChain.one()).thenReturn(newUser(1L, PHONE, ""));

        Result r = userService.login(form("123456", null), null);

        assertTrue(r.getSuccess());
        assertTrue(r.getData() instanceof String);
        // 验证码一次性使用
        verify(stringRedisTemplate).delete("login:code:" + PHONE);
        // 签发登录态：token hash + TTL
        verify(hashOperations).putAll(anyString(), any());
        verify(stringRedisTemplate).expire(anyString(), anyLong(), any());
    }

    @Test
    void login_验证码错误时拒绝() {
        when(valueOperations.get("login:code:" + PHONE)).thenReturn("123456");

        Result r = userService.login(form("999999", null), null);

        assertEquals("验证码错误", r.getErrorMsg());
    }

    // ==================== login：密码登录 ====================

    @Test
    void login_密码正确时登录成功() {
        String encoded = PasswordEncoder.encode("abc12345");
        when(queryChain.one()).thenReturn(newUser(1L, PHONE, encoded));

        Result r = userService.login(form(null, "abc12345"), null);

        assertTrue(r.getSuccess());
        assertTrue(r.getData() instanceof String);
        // 成功后清除失败计数
        verify(stringRedisTemplate).delete(FAIL_KEY);
    }

    @Test
    void login_密码错误时返回剩余次数() {
        String encoded = PasswordEncoder.encode("abc12345");
        when(queryChain.one()).thenReturn(newUser(1L, PHONE, encoded));
        when(valueOperations.increment(FAIL_KEY)).thenReturn(1L);

        Result r = userService.login(form(null, "wrong-pass"), null);

        assertEquals("密码错误，还可尝试 4 次", r.getErrorMsg());
        // 首次失败设置窗口过期时间
        verify(stringRedisTemplate).expire(eq(FAIL_KEY), anyLong(), any());
    }

    @Test
    void login_连续失败达上限后锁定() {
        when(valueOperations.get(FAIL_KEY)).thenReturn("5");

        Result r = userService.login(form(null, "abc12345"), null);

        assertEquals("密码错误次数过多，请 10 分钟后再试，或使用验证码登录", r.getErrorMsg());
        // 锁定期内不再查询用户/校验密码
        verify(queryChain, never()).one();
    }

    @Test
    void login_用户未注册时拒绝密码登录() {
        Result r = userService.login(form(null, "abc12345"), null);

        assertEquals("该手机号未注册，请使用验证码登录", r.getErrorMsg());
    }

    @Test
    void login_用户未设置密码时引导验证码登录() {
        when(queryChain.one()).thenReturn(newUser(1L, PHONE, ""));

        Result r = userService.login(form(null, "abc12345"), null);

        assertEquals("该账号尚未设置密码，请先用验证码登录后在「我的-修改密码」中设置", r.getErrorMsg());
    }

    // ==================== setPassword：设置/修改密码 ====================

    @Test
    void setPassword_新密码格式非法时拒绝() {
        UserHolder.saveUser(new UserDTO());

        Result r = userService.setPassword(null, "12");

        assertEquals("密码格式不正确（4~32位字母、数字或下划线）", r.getErrorMsg());
    }

    @Test
    void setPassword_未设置过密码时无需原密码直接设置() {
        UserDTO dto = new UserDTO();
        dto.setId(1L);
        UserHolder.saveUser(dto);
        doReturn(newUser(1L, PHONE, "")).when(userService).getById(1L);

        Result r = userService.setPassword(null, "abc12345");

        assertTrue(r.getSuccess());
        // 方法引用每次创建新 lambda 实例，无法用 eq() 精确匹配，仅验证第二参数
        verify(updateChain).set(any(), anyString());
    }

    @Test
    void setPassword_已有密码且原密码错误时拒绝() {
        UserDTO dto = new UserDTO();
        dto.setId(1L);
        UserHolder.saveUser(dto);
        doReturn(newUser(1L, PHONE, PasswordEncoder.encode("old-pass-1"))).when(userService).getById(1L);

        Result r = userService.setPassword("wrong-old", "abc12345");

        assertEquals("原密码错误", r.getErrorMsg());
        verify(updateChain, never()).update();
    }

    @Test
    void setPassword_已有密码且原密码正确时成功() {
        UserDTO dto = new UserDTO();
        dto.setId(1L);
        UserHolder.saveUser(dto);
        doReturn(newUser(1L, PHONE, PasswordEncoder.encode("old-pass-1"))).when(userService).getById(1L);

        Result r = userService.setPassword("old-pass-1", "abc12345");

        assertTrue(r.getSuccess());
        // 方法引用每次创建新 lambda 实例，无法用 eq() 精确匹配，仅验证第二参数
        verify(updateChain).set(any(), anyString());
    }

    @Test
    void setPassword_更新失败时返回失败() {
        UserDTO dto = new UserDTO();
        dto.setId(1L);
        UserHolder.saveUser(dto);
        doReturn(newUser(1L, PHONE, "")).when(userService).getById(1L);
        when(updateChain.update()).thenReturn(false);

        Result r = userService.setPassword(null, "abc12345");

        assertEquals("密码修改失败，请重试", r.getErrorMsg());
    }
}
