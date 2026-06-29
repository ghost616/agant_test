package com.ghost616.platform.enums;

/**
 * 统一错误码枚举。
 */
public enum ErrorCode {

    SYSTEM_ERROR("SYS-001", "系统内部错误"),
    PARAM_INVALID("SYS-002", "参数校验失败"),
    NOT_FOUND("SYS-003", "资源不存在"),
    UNAUTHORIZED("SYS-004", "未授权访问"),
    DUPLICATE_KEY("SYS-005", "数据重复"),

    MODEL_INVOKE_ERROR("MODEL-INVOKE-001", "模型调用失败"),
    MODEL_VERIFY_ERROR("MODEL-VERIFY-001", "模型连通性验证失败"),
    MODEL_UNSUPPORTED("MODEL-UNSUPPORTED-001", "不支持的平台类型"),
    MODEL_NOT_FOUND("MODEL-CONFIG-001", "模型配置不存在"),
    MODEL_ALREADY_EXISTS("MODEL-CONFIG-002", "模型名称已存在"),

    TOOL_NOT_FOUND("TOOL-CONFIG-001", "工具配置不存在"),
    TOOL_ALREADY_EXISTS("TOOL-CONFIG-002", "工具名称已存在"),
    TOOL_SCHEMA_INVALID("TOOL-CONFIG-003", "工具参数 Schema 不合法"),
    TOOL_INVOKE_ERROR("TOOL-INVOKE-001", "工具调用初始化失败"),
    TOOL_RUNTIME_NOT_FOUND("TOOL-RUNTIME-001", "脚本运行时环境不可用"),
    TOOL_EXECUTE_TIMEOUT("TOOL-EXEC-001", "工具执行超时"),
    TOOL_EXECUTE_ERROR("TOOL-EXEC-002", "工具执行失败"),

    AGENT_NOT_FOUND("AGENT-CONFIG-001", "智能体配置不存在"),
    AGENT_ALREADY_EXISTS("AGENT-CONFIG-002", "智能体名称已存在"),

    SKILL_NOT_FOUND("SKILL-CONFIG-001", "SKILL 配置不存在"),
    SKILL_ALREADY_EXISTS("SKILL-CONFIG-002", "SKILL 名称已存在"),

    SESSION_NOT_FOUND("SESSION-001", "会话不存在");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
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
