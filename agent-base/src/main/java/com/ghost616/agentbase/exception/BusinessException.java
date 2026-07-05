package com.ghost616.agentbase.exception;

import com.ghost616.agentbase.enums.ErrorCode;

/**
 * 业务逻辑异常。
 */
public class BusinessException extends BaseException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
