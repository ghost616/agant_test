package com.ghost616.agentbase.exception;

import com.ghost616.agentbase.enums.ErrorCode;

/**
 * 平台统一基础异常。
 */
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    public BaseException(ErrorCode errorCode, String detail) {
        super(detail != null ? detail : errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detail;
    }
}
