package com.ghost616.agentbase.enums;

/**
 * 智能体日志类型枚举。
 */
public enum LogType {

    REQUEST_ENTRY("REQUEST_ENTRY", "请求入口"),
    CALL_SOURCE("CALL_SOURCE", "调用来源");

    private final String code;
    private final String description;

    LogType(String code, String description) {
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
