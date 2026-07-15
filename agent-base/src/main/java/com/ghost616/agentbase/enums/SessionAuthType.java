package com.ghost616.agentbase.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 会话授权类型枚举，定义会话的授权范围。
 */
public enum SessionAuthType {

    ALL(0, "所有会话都可使用"),
    PARENT(1, "父会话使用"),
    CHILD(2, "子会话使用");

    @EnumValue
    private final Integer code;
    private final String description;

    SessionAuthType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
