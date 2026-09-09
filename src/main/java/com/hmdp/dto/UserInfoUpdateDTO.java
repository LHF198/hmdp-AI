package com.hmdp.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料请求体：昵称、头像（tb_user）+ 城市、介绍、性别、生日（tb_user_info）
 */
@Data
public class UserInfoUpdateDTO {

    /** 昵称（tb_user，非空才更新） */
    private String nickName;

    /** 头像地址（tb_user，非空才更新） */
    private String icon;

    /** 城市名称 */
    private String city;

    /** 个人介绍（不超过128个字符） */
    @Size(max = 128, message = "个人介绍过长（最多128字）")
    private String introduce;

    /** 性别，0：男，1：女 */
    private Boolean gender;

    /** 生日 */
    private LocalDate birthday;
}
