package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionErrorLogDataTest {

    @Test
    void logType应返回ERROR_LOG() {
        SessionErrorLogData data = SessionErrorLogData.builder().build();
        assertEquals(LogType.ERROR_LOG, data.logType());
    }

    @Test
    void 字段应默认为null() {
        SessionErrorLogData data = SessionErrorLogData.builder().build();
        assertNull(data.getSessionId());
        assertNull(data.getConversationId());
        assertNull(data.getLogLevel());
        assertNull(data.getErrorCode());
        assertNull(data.getMessage());
        assertNull(data.getException());
    }

    @Test
    void builder应正确设置继承与自有字段() {
        RuntimeException exception = new RuntimeException("boom");
        SessionErrorLogData data = SessionErrorLogData.builder()
                .sessionId("s1")
                .conversationId("conv-1")
                .errorCode("SYS-001")
                .message("出错")
                .exception(exception)
                .build();

        assertEquals("s1", data.getSessionId());
        assertEquals("conv-1", data.getConversationId());
        assertEquals("SYS-001", data.getErrorCode());
        assertEquals("出错", data.getMessage());
        assertSame(exception, data.getException());
    }

    @Test
    void 不应继承ContextLogData() {
        SessionErrorLogData data = SessionErrorLogData.builder().build();
        assertFalse(ContextLogData.class.isAssignableFrom(data.getClass()));
        assertTrue(SessionLogData.class.isAssignableFrom(data.getClass()));
    }
}
