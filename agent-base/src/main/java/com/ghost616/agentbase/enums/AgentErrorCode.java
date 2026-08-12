package com.ghost616.agentbase.enums;

/**
 * 智能体模块统一错误码枚举。
 * 仅包含 agent-base 与 agent-integration 实际使用的错误码。
 */
public enum AgentErrorCode {

    SYSTEM_ERROR("SYS-001", "系统内部错误"),
    PARAM_INVALID("SYS-002", "参数校验失败"),
    NOT_FOUND("SYS-003", "资源不存在"),
    DUPLICATE_KEY("SYS-005", "数据重复"),

    MODEL_NOT_FOUND("MODEL-CONFIG-001", "模型配置不存在"),
    MODEL_INVOKE_ERROR("MODEL-INVOKE-001", "模型调用失败"),
    MODEL_VERIFY_ERROR("MODEL-VERIFY-001", "模型连通性验证失败"),

    TOOL_INVOKE_ERROR("TOOL-INVOKE-001", "工具调用初始化失败"),
    TOOL_RUNTIME_NOT_FOUND("TOOL-RUNTIME-001", "脚本运行时环境不可用"),
    TOOL_EXECUTE_TIMEOUT("TOOL-EXEC-001", "工具执行超时"),
    TOOL_EXECUTE_ERROR("TOOL-EXEC-002", "工具执行失败"),

    SESSION_NOT_FOUND("SESSION-001", "会话不存在");

    private final String code;
    private final String message;

    AgentErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
