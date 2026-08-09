package com.ghost616.agentbase.enums;

/**
 * 智能体日志级别枚举。
 */
public enum LogLevel {

    INFO("INFO", "信息"),
    WARN("WARN", "警告"),
    ERROR("ERROR", "错误");

    private final String code;
    private final String description;

    LogLevel(String code, String description) {
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
