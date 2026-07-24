package com.ghost616.platform.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 子工具类型枚举。
 */
public enum SubToolType {

    BROWSER("BROWSER", "浏览器");

    @EnumValue
    private final String code;
    private final String description;

    SubToolType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
