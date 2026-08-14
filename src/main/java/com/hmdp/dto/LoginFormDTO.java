package com.hmdp.dto;

import com.hmdp.utils.RegexPatterns;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginFormDTO {

    /** 手机号（必填，且需为合法手机号） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = RegexPatterns.PHONE_REGEX, message = "手机号格式不正确")
    private String phone;

    /** 验证码（短信登录方式；密码登录时无需填写） */
    private String code;

    /** 密码（密码登录方式；短信登录时无需填写） */
    private String password;
}
