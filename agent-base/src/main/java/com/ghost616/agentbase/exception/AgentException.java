package com.ghost616.agentbase.exception;

import com.ghost616.agentbase.enums.AgentErrorCode;

/**
 * 智能体统一基础异常。
 */
public class AgentException extends RuntimeException {

    private final AgentErrorCode errorCode;
    private final String detail;

    public AgentException(AgentErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    public AgentException(AgentErrorCode errorCode, String detail) {
        super(detail != null ? detail : errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public AgentErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detail;
    }
}
