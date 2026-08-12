package com.ghost616.platform.exception;

import com.ghost616.platform.enums.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessExceptionTest {

    @Test
    void extendsBaseException() {
        assertTrue(BaseException.class.isAssignableFrom(BusinessException.class));
    }

    @Test
    void singleArgConstructor() {
        BusinessException ex = new BusinessException(ErrorCode.TOOL_NOT_FOUND);
        assertEquals(ErrorCode.TOOL_NOT_FOUND, ex.getErrorCode());
        assertNull(ex.getDetail());
        assertEquals("工具配置不存在", ex.getMessage());
    }

    @Test
    void twoArgConstructorWithDetail() {
        BusinessException ex = new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具 ID 非法");
        assertEquals(ErrorCode.TOOL_NOT_FOUND, ex.getErrorCode());
        assertEquals("工具 ID 非法", ex.getDetail());
        assertEquals("工具 ID 非法", ex.getMessage());
    }
}
