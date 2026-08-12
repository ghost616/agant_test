package com.ghost616.platform.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 聚合类型枚举。
 */
public enum AggregationType {

    GROUP("GROUP", "分组聚合"),
    DAILY("DAILY", "按日聚合");

    @EnumValue
    private final String code;
    private final String description;

    AggregationType(String code, String description) {
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
