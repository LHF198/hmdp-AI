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

    /**
     * 设置/修改当前登录用户的密码（首次设置无需旧密码；已有密码时必须校验旧密码）
     *
     * @param oldPassword 原密码（账号未设置过密码时可传 null/空）
     * @param newPassword 新密码（4~32 位字母、数字或下划线）
     * @return 操作结果
     */
    Result setPassword(String oldPassword, String newPassword);

}
