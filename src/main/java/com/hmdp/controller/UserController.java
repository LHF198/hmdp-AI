package com.hmdp.controller;


import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hmdp.annotation.Anonymous;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import com.hmdp.utils.UserHolder;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送手机验证码
     */
    @Anonymous
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // 发送短信验证码并保存验证码
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @Anonymous
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginFormDTO loginForm, HttpSession session){
        // 实现登录功能
        return userService.login(loginForm, session);
    }

    /**
     * 登出功能：删除Redis中的登录token，使其立即失效
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request){
        // 1.获取请求头中的token
        String token = request.getHeader("authorization");
        // 2.删除Redis中的token对应的用户信息
        if (StrUtil.isNotBlank(token)) {
            stringRedisTemplate.delete(LOGIN_USER_KEY + token);
            log.info("用户登出: token={}", token);
        }
        return Result.ok();
    }

    @GetMapping("/me")
    public Result me(){
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    /**
     * 设置/修改当前登录用户的密码（需登录；已有密码时必须携带原密码）
     * @param body { oldPassword?, newPassword }
     */
    @PutMapping("/password")
    public Result updatePassword(@RequestBody Map<String, Object> body) {
        Object oldPassword = body.get("oldPassword");
        Object newPassword = body.get("newPassword");
        return userService.setPassword(
                oldPassword == null ? null : oldPassword.toString(),
                newPassword == null ? null : newPassword.toString());
    }

    /**
     * 修改当前登录用户的个人资料
     * @param body 待修改的资料，支持昵称、城市、介绍、性别、生日
     */
    @PutMapping("/info")
    public Result updateUserInfo(@RequestBody Map<String, Object> body, HttpServletRequest request){
        Long userId = UserHolder.getUser().getId();
        // 1.修改昵称、头像（tb_user表），并同步Redis登录态，保证 /user/me 立即生效
        String token = request.getHeader("authorization");
        Object nickName = body.get("nickName");
        Object icon = body.get("icon");
        userService.updateProfile(userId,
                nickName == null ? null : nickName.toString(),
                icon == null ? null : icon.toString(),
                token);
        // 2.修改个人资料（tb_user_info表），强制绑定当前登录用户，防止越权修改
        UserInfo info = BeanUtil.toBean(body, UserInfo.class);
        info.setUserId(userId);
        // 不允许通过该接口修改的字段置空
        info.setFans(null);
        info.setFollowee(null);
        info.setCredits(null);
        info.setLevel(null);
        // saveOrUpdate：没有记录则新增，有记录则更新
        try {
            boolean success = userInfoService.saveOrUpdate(info);
            if (!success) {
                log.error("个人资料更新失败: userId={}, info={}", userId, info);
            }
            return success ? Result.ok() : Result.fail("更新失败");
        } catch (Exception e) {
            log.error("个人资料更新异常: userId={}", userId, e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }

    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }
}