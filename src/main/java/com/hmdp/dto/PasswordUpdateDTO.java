package com.hmdp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求体
 */
@Data
public class PasswordUpdateDTO {

    /** 原密码（账号未设置过密码时可不传） */
    private String oldPassword;

    /** 新密码（必填，格式由服务层校验） */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
