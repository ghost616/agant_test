package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageQueryLogDataTest {

    @Test
    void logType应返回MESSAGE_QUERY() {
        MessageQueryLogData data = MessageQueryLogData.builder().build();
        assertEquals(LogType.MESSAGE_QUERY, data.logType());
    }

    @Test
    void 数字字段应默认为0() {
        MessageQueryLogData data = MessageQueryLogData.builder().build();
        assertEquals(0, data.getMessageCount());
        assertNull(data.getSessionId());
        assertNull(data.getConversationId());
        assertTrue(SessionLogData.class.isAssignableFrom(data.getClass()));
    }

    @Test
    void builder应正确设置继承与自有字段() {
        MessageQueryLogData data = MessageQueryLogData.builder()
                .sessionId("s1")
                .conversationId("conv-1")
                .messageCount(10)
                .build();

        assertEquals("s1", data.getSessionId());
        assertEquals("conv-1", data.getConversationId());
        assertEquals(10, data.getMessageCount());
    }
}
