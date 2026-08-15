package com.ghost616.platform.util;

import cn.hutool.crypto.SmUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PasswordUtil 单元测试。
 *
 * <p>覆盖：输出格式（64 位小写 hex）、确定性、公式正确性（独立复算 MD5 + SM3）、
 * 输入敏感性（明文密码/用户ID/创建时间）、空参数校验、边界值（时间戳极值、超长密码）。</p>
 */
class PasswordUtilTest {

    private static final Pattern LOWERCASE_HEX_64 = Pattern.compile("^[0-9a-f]{64}$");

    // ---------- 正向覆盖 ----------

    @Test
    void encrypt输出为64位小写十六进制() {
        String result = PasswordUtil.encrypt("abc123", "user-1", 1700000000000L);
        assertTrue(LOWERCASE_HEX_64.matcher(result).matches(),
                "输出应为 64 位小写 hex，实际: " + result);
    }

    @Test
    void 相同输入输出一致() {
        String r1 = PasswordUtil.encrypt("p@ssw0rd", "u001", 123456789L);
        String r2 = PasswordUtil.encrypt("p@ssw0rd", "u001", 123456789L);
        assertEquals(r1, r2);
    }

    @Test
    void 加密公式与独立复算一致() throws Exception {
        // 独立复算：MD5(明文密码 + 用户ID) 32 位小写 hex，再 SM3(MD5hex + 创建时间)
        String plain = "MyP@ss-2026";
        String userId = "uid-42";
        long createTime = 1760000000000L;

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] md5Bytes = md5.digest((plain + userId).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : md5Bytes) {
            sb.append(String.format("%02x", b));
        }
        String md5Hex = sb.toString();
        assertEquals(32, md5Hex.length());

        String expected = SmUtil.sm3(md5Hex + createTime);
        String actual = PasswordUtil.encrypt(plain, userId, createTime);
        assertEquals(expected, actual);
    }

    @Test
    void 不同明文密码输出不同() {
        String a = PasswordUtil.encrypt("password-A", "u1", 1000L);
        String b = PasswordUtil.encrypt("password-B", "u1", 1000L);
        assertNotEquals(a, b);
    }

    @Test
    void 不同用户ID输出不同() {
        String a = PasswordUtil.encrypt("same-pass", "u1", 1000L);
        String b = PasswordUtil.encrypt("same-pass", "u2", 1000L);
        assertNotEquals(a, b);
    }

    @Test
    void 不同创建时间输出不同() {
        String a = PasswordUtil.encrypt("same-pass", "u1", 1000L);
        String b = PasswordUtil.encrypt("same-pass", "u1", 1001L);
        assertNotEquals(a, b);
    }

    @Test
    void 创建时间毫秒差1输出不同() {
        String a = PasswordUtil.encrypt("p", "u", 1700000000000L);
        String b = PasswordUtil.encrypt("p", "u", 1700000000001L);
        assertNotEquals(a, b);
    }

    // ---------- 反向覆盖（参数校验） ----------

    @Test
    void 明文密码为null抛IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordUtil.encrypt(null, "u1", 1000L));
    }

    @Test
    void 明文密码为空字符串抛IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordUtil.encrypt("", "u1", 1000L));
    }

    @Test
    void 明文密码为空白抛IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordUtil.encrypt("   ", "u1", 1000L));
    }

    @Test
    void 明文密码为制表符空白抛IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordUtil.encrypt("\t\n ", "u1", 1000L));
    }

    @Test
    void 用户ID为null抛IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordUtil.encrypt("pass", null, 1000L));
    }

    @Test
    void 用户ID为空字符串抛IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordUtil.encrypt("pass", "", 1000L));
    }

    @Test
    void 用户ID为空白抛IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordUtil.encrypt("pass", "  ", 1000L));
    }

    // ---------- 边界值与极限值 ----------

    @Test
    void 创建时间为0不抛异常且输出合法() {
        String result = PasswordUtil.encrypt("pass", "u", 0L);
        assertTrue(LOWERCASE_HEX_64.matcher(result).matches());
    }

    @Test
    void 创建时间为负数不抛异常且输出合法() {
        String result = PasswordUtil.encrypt("pass", "u", -1L);
        assertTrue(LOWERCASE_HEX_64.matcher(result).matches());
    }

    @Test
    void 创建时间为Long最大值不抛异常且输出合法() {
        String result = PasswordUtil.encrypt("pass", "u", Long.MAX_VALUE);
        assertTrue(LOWERCASE_HEX_64.matcher(result).matches());
    }

    @Test
    void 创建时间为Long最小值不抛异常且输出合法() {
        String result = PasswordUtil.encrypt("pass", "u", Long.MIN_VALUE);
        assertTrue(LOWERCASE_HEX_64.matcher(result).matches());
    }

    @Test
    void 超长密码10000字符可正常加密() {
        String longPass = "p".repeat(10000);
        String result = PasswordUtil.encrypt(longPass, "u", 1000L);
        assertTrue(LOWERCASE_HEX_64.matcher(result).matches());
    }

    @Test
    void 含中文与特殊字符的密码可正常加密() {
        String result = PasswordUtil.encrypt("密碼P@ss!#$%^&*()中文", "用户ID-甲", 999L);
        assertTrue(LOWERCASE_HEX_64.matcher(result).matches());
    }

    @Test
    void 单字符输入可正常加密() {
        String result = PasswordUtil.encrypt("a", "b", 1L);
        assertTrue(LOWERCASE_HEX_64.matcher(result).matches());
    }
}