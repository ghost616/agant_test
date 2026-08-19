package com.ghost616.agentbase.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 子会话打开方式枚举，定义子会话的打开/推送方式。
 */
public enum SubSessionOpenMode {

    WEBSOCKET("WEBSOCKET", "WebSocket推送"),
    TOOL_CALL("TOOL_CALL", "前台工具调用");

    /**
     * 默认打开方式，语义为前台工具调用。
     */
    public static final SubSessionOpenMode DEFAULT = TOOL_CALL;

    @EnumValue
    private final String code;
    private final String description;

    SubSessionOpenMode(String code, String description) {
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