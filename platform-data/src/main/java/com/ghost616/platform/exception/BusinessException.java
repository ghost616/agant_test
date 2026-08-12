package com.ghost616.platform.exception;

import com.ghost616.platform.enums.ErrorCode;

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
