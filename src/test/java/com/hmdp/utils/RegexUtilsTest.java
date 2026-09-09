package com.hmdp.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 正则校验工具单元测试（纯 JUnit，无环境依赖）
 */
class RegexUtilsTest {

    @Test
    void isPhoneInvalid_shouldRejectInvalidPhones() {
        // 空值 / 位数不足 / 非法号段 / 非数字
        assertTrue(RegexUtils.isPhoneInvalid(null));
        assertTrue(RegexUtils.isPhoneInvalid(""));
        assertTrue(RegexUtils.isPhoneInvalid("  "));
        assertTrue(RegexUtils.isPhoneInvalid("12345678901"));
        assertTrue(RegexUtils.isPhoneInvalid("1381234567"));
        assertTrue(RegexUtils.isPhoneInvalid("138123456789"));
        assertTrue(RegexUtils.isPhoneInvalid("12812345678"));
        assertTrue(RegexUtils.isPhoneInvalid("1381234abcd"));
    }

    @Test
    void isPhoneInvalid_shouldAcceptValidPhones() {
        // 常见合法号段
        assertFalse(RegexUtils.isPhoneInvalid("13812345678"));
        assertFalse(RegexUtils.isPhoneInvalid("18912345678"));
        assertFalse(RegexUtils.isPhoneInvalid("19912345678"));
        assertFalse(RegexUtils.isPhoneInvalid("15812345678"));
        assertFalse(RegexUtils.isPhoneInvalid("16612345678"));
    }
}
