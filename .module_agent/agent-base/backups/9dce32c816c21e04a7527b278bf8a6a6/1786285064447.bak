package com.ghost616.agentbase.enums;

/**
 * 智能体日志类型枚举。
 */
public enum LogType {

    REQUEST_ENTRY("REQUEST_ENTRY", "请求入口"),
    CALL_SOURCE("CALL_SOURCE", "调用来源"),
    ERROR_LOG("ERROR_LOG", "错误日志"),
    ROUTE("ROUTE", "路由分发"),
    MODEL_CALL("MODEL_CALL", "模型调用"),
    STREAM_EVENT("STREAM_EVENT", "流式事件"),
    HISTORY_EXPAND("HISTORY_EXPAND", "历史展开"),
    SKILL_LOAD("SKILL_LOAD", "技能加载"),
    CONTEXT_BUILD("CONTEXT_BUILD", "上下文构建"),
    CHILD_SESSION("CHILD_SESSION", "子会话创建"),
    REFRESH("REFRESH", "上下文刷新"),
    HANDLE_MESSAGE("HANDLE_MESSAGE", "消息处理"),
    CACHE_REMOVE("CACHE_REMOVE", "缓存移除"),
    SEND_MESSAGE("SEND_MESSAGE", "消息发送"),
    MESSAGE_SAVE("MESSAGE_SAVE", "消息保存"),
    MESSAGE_QUERY("MESSAGE_QUERY", "消息查询"),
    MESSAGE_ROLLBACK("MESSAGE_ROLLBACK", "消息回退"),
    TOOL_EXECUTE("TOOL_EXECUTE", "工具执行"),
    TOOL_CONTINUE("TOOL_CONTINUE", "工具执行后继续");

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
