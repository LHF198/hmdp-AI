package com.hmdp.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PasswordEncoder 单元测试：验证 BCrypt 新格式编码与旧 MD5 格式兼容校验。
 */
class PasswordEncoderTest {

    // ==================== BCrypt 新格式 ====================

    @Test
    void encode_新密码使用BCrypt格式() {
        String encoded = PasswordEncoder.encode("abc12345");

        // BCrypt 格式以 $2a$ 开头，不含 "@"
        assertTrue(encoded.startsWith("$2a$"), "BCrypt 编码应以 $2a$ 开头: " + encoded);
        assertFalse(encoded.contains("@"), "BCrypt 编码不应包含 @");
    }

    @Test
    void matches_BCrypt格式密码正确时返回true() {
        String encoded = PasswordEncoder.encode("abc12345");

        assertTrue(PasswordEncoder.matches(encoded, "abc12345"));
    }

    @Test
    void matches_BCrypt格式密码错误时返回false() {
        String encoded = PasswordEncoder.encode("abc12345");

        assertFalse(PasswordEncoder.matches(encoded, "wrong-password"));
    }

    @Test
    void encode_同一密码每次生成不同哈希() {
        String encoded1 = PasswordEncoder.encode("abc12345");
        String encoded2 = PasswordEncoder.encode("abc12345");

        // BCrypt 每次加密使用随机盐，结果不同
        assertNotEquals(encoded1, encoded2);
        // 但都能校验通过
        assertTrue(PasswordEncoder.matches(encoded1, "abc12345"));
        assertTrue(PasswordEncoder.matches(encoded2, "abc12345"));
    }

    // ==================== 旧 MD5 格式兼容 ====================

    @Test
    void matches_旧MD5格式密码正确时返回true() {
        // 模拟旧格式：salt@md5hex
        String salt = "abcdefghij1234567890";
        String md5hex = org.springframework.util.DigestUtils.md5DigestAsHex(
                ("abc12345" + salt).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String oldFormat = salt + "@" + md5hex;

        assertTrue(PasswordEncoder.matches(oldFormat, "abc12345"));
    }

    @Test
    void matches_旧MD5格式密码错误时返回false() {
        String salt = "abcdefghij1234567890";
        String md5hex = org.springframework.util.DigestUtils.md5DigestAsHex(
                ("abc12345" + salt).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String oldFormat = salt + "@" + md5hex;

        assertFalse(PasswordEncoder.matches(oldFormat, "wrong-password"));
    }

    // ==================== 边界条件 ====================

    @Test
    void matches_存储密码为null时返回false() {
        assertFalse(PasswordEncoder.matches(null, "abc12345"));
    }

    @Test
    void matches_原始密码为null时返回false() {
        String encoded = PasswordEncoder.encode("abc12345");
        assertFalse(PasswordEncoder.matches(encoded, null));
    }
}
