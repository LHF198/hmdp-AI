package com.hmdp.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

import jakarta.servlet.http.HttpSession;

/**
 * <p>
 * 用户服务接口：发送验证码、手机号+验证码登录/注册、密码登录、查询用户
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sign();

    Result signCount();

    /**
     * 修改昵称/头像（非空字段才更新），并同步 Redis 登录态缓存保证 /user/me 立即生效
     *
     * @param userId   当前登录用户id
     * @param nickName 新昵称，可为 null
     * @param icon     新头像，可为 null
     * @param token    登录 token（可能为空，为空时跳过 Redis 同步）
     */
    void updateProfile(Long userId, String nickName, String icon, String token);

}
