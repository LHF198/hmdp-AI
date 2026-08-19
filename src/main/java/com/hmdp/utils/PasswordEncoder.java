package com.hmdp.utils;


import cn.hutool.core.util.RandomUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码编码器：新密码使用 BCrypt（安全），旧密码格式（salt@md5hex）兼容校验。
 * <p>
 * 迁移策略：{@code matches()} 根据是否包含 "@" 区分新旧格式，
 * 新注册/改密自动升级为 BCrypt，旧密码在下次登录成功后可引导用户重置。
 */
public class PasswordEncoder {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    /**
     * 新密码使用 BCrypt 哈希（自动生成随机盐，结果含算法标识）
     */
    public static String encode(String password) {
        return BCRYPT.encode(password);
    }

    /**
     * 校验密码：根据存储格式自动选择算法
     * <ul>
     *   <li>含 "@" → 旧格式 salt@md5hex，走遗留 MD5 校验</li>
     *   <li>不含 "@" → BCrypt 格式（$2a$...），走 BCrypt 校验</li>
     * </ul>
     */
    public static Boolean matches(String encodedPassword, String rawPassword) {
        if (encodedPassword == null || rawPassword == null) {
            return false;
        }
        if (encodedPassword.contains("@")) {
            // 旧格式：salt@md5hex
            return encodedPassword.equals(legacyMd5Encode(rawPassword, encodedPassword.split("@")[0]));
        }
        // 新格式：BCrypt
        return BCRYPT.matches(rawPassword, encodedPassword);
    }

    /**
     * 遗留 MD5 编码（仅供旧密码兼容校验使用，新密码禁止调用此方法）
     */
    private static String legacyMd5Encode(String password, String salt) {
        return salt + "@" + org.springframework.util.DigestUtils.md5DigestAsHex(
                (password + salt).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
