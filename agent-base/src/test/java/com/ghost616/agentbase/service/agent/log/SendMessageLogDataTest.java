package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SendMessageLogDataTest {

    @Test
    void logType应返回SEND_MESSAGE() {
        SendMessageLogData data = SendMessageLogData.builder().build();
        assertEquals(LogType.SEND_MESSAGE, data.logType());
    }

    @Test
    void 字段应默认为null() {
        SendMessageLogData data = SendMessageLogData.builder().build();
        assertNull(data.getLogLevel());
        assertNull(data.getSessionId());
        assertNull(data.getConversationId());
        assertNull(data.getChildSessionId());
        assertNull(data.getContent());
        assertNull(data.getModelId());
        assertNull(data.getThinking());
    }

    @Test
    void builder应正确设置字段() {
        SendMessageLogData data = SendMessageLogData.builder()
                .sessionId("s1")
                .childSessionId("c1")
                .content("hello")
                .modelId("300")
                .thinking(true)
                .build();

        assertEquals("s1", data.getSessionId());
        assertEquals("c1", data.getChildSessionId());
        assertEquals("hello", data.getContent());
        assertEquals("300", data.getModelId());
        assertTrue(data.getThinking());
    }

    @Test
    void 不应继承ContextLogData() {
        SendMessageLogData data = SendMessageLogData.builder().build();
        assertFalse(ContextLogData.class.isAssignableFrom(data.getClass()));
        assertTrue(SessionLogData.class.isAssignableFrom(data.getClass()));
    }
}
