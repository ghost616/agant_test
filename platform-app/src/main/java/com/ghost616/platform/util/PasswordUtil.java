package com.ghost616.platform.util;

import cn.hutool.crypto.SmUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码工具类，提供基于国密 SM3 算法的密码加密能力。
 *
 * <p>加密公式：SM3( MD5(明文密码 + 用户ID) + 创建时间毫秒时间戳 )，其中
 * MD5 使用 JDK MessageDigest 计算，SM3 使用 hutool SmUtil 计算。</p>
 */
public final class PasswordUtil {

    /** MD5 算法名称。 */
    private static final String MD5_ALGORITHM = "MD5";

    /** 十六进制字符表（小写）。 */
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /** 每个字节对应的十六进制字符数。 */
    private static final int HEX_CHARS_PER_BYTE = 2;

    private PasswordUtil() {
    }

    /**
     * 根据明文密码、用户 ID 和创建时间加密生成密码摘要。
     *
     * <p>加密流程：先计算 MD5(明文密码 + 用户ID) 的十六进制字符串，
     * 再对 MD5 结果拼接创建时间毫秒时间戳后执行 SM3 摘要。</p>
     *
     * @param plainPassword 明文密码，不允许为空
     * @param userId        用户 ID，不允许为空
     * @param createTime    创建时间（epoch 毫秒时间戳）
     * @return 加密后的密码摘要（SM3 十六进制字符串，64 位）
     * @throws IllegalArgumentException 明文密码或用户 ID 为空时抛出
     * @throws IllegalStateException    MD5 算法在当前 JVM 不可用时抛出
     */
    public static String encrypt(String plainPassword, String userId, long createTime) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("明文密码不能为空");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        String md5Hex = md5Hex(plainPassword + userId);
        return SmUtil.sm3(md5Hex + createTime);
    }

    /**
     * 计算输入字符串的 MD5 十六进制摘要。
     *
     * @param input 输入字符串
     * @return MD5 十六进制字符串（32 位，小写）
     * @throws IllegalStateException MD5 算法在当前 JVM 不可用时抛出
     */
    private static String md5Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(MD5_ALGORITHM);
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }

    /**
     * 将字节数组编码为小写十六进制字符串。
     *
     * @param bytes 字节数组
     * @return 小写十六进制字符串
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * HEX_CHARS_PER_BYTE);
        for (byte b : bytes) {
            sb.append(HEX_CHARS[(b >> 4) & 0x0F]);
            sb.append(HEX_CHARS[b & 0x0F]);
        }
        return sb.toString();
    }
}
