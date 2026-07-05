package com.ghost616.agentbase.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 通用启用/禁用状态枚举。
 */
public enum CommonStatus {

    ENABLED("ENABLED", "启用"),
    DISABLED("DISABLED", "禁用");

    @EnumValue
    private final String code;
    private final String description;

    CommonStatus(String code, String description) {
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
