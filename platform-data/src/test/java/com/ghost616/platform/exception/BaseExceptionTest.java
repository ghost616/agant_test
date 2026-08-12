package com.ghost616.platform.exception;

import com.ghost616.platform.enums.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseExceptionTest {

    @Test
    void singleArgConstructor() {
        BaseException ex = new BaseException(ErrorCode.AGENT_NOT_FOUND);
        assertEquals(ErrorCode.AGENT_NOT_FOUND, ex.getErrorCode());
        assertNull(ex.getDetail());
        assertEquals("智能体配置不存在", ex.getMessage());
    }

    @Test
    void twoArgConstructorWithDetail() {
        BaseException ex = new BaseException(ErrorCode.AGENT_NOT_FOUND, "自定义详情");
        assertEquals(ErrorCode.AGENT_NOT_FOUND, ex.getErrorCode());
        assertEquals("自定义详情", ex.getDetail());
        assertEquals("自定义详情", ex.getMessage());
    }

    @Test
    void twoArgConstructorWithNullDetail() {
        BaseException ex = new BaseException(ErrorCode.AGENT_NOT_FOUND, null);
        assertEquals(ErrorCode.AGENT_NOT_FOUND, ex.getErrorCode());
        assertNull(ex.getDetail());
        assertEquals("智能体配置不存在", ex.getMessage());
    }

    @Test
    void extendsRuntimeException() {
        assertTrue(RuntimeException.class.isAssignableFrom(BaseException.class));
    }
}
