package com.hmdp.utils;

import com.hmdp.dto.UserDTO;

/**
 * 当前登录用户持有者：基于 ThreadLocal 存取，由拦截器在请求开始时写入、
 * 请求结束时清除，供任意业务层随时获取当前用户
 */
public class UserHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    public static UserDTO getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
