package com.hmdp.dto;

import lombok.Data;

/**
 * 登录用户轻量信息：存入 Redis 会话与 ThreadLocal，避免将完整 User 透出到各处
 */
@Data
public class UserDTO {

    private Long id;
    private String nickName;
    private String icon;
}
