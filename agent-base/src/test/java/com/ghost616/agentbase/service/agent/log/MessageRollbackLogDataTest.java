package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageRollbackLogDataTest {

    @Test
    void logType应返回MESSAGE_ROLLBACK() {
        MessageRollbackLogData data = MessageRollbackLogData.builder().build();
        assertEquals(LogType.MESSAGE_ROLLBACK, data.logType());
    }

    @Test
    void 数字字段应默认为0() {
        MessageRollbackLogData data = MessageRollbackLogData.builder().build();
        assertEquals(0, data.getRollbackCount());
        assertNull(data.getSessionId());
        assertNull(data.getConversationId());
        assertTrue(SessionLogData.class.isAssignableFrom(data.getClass()));
    }

    @Test
    void builder应正确设置继承与自有字段() {
        MessageRollbackLogData data = MessageRollbackLogData.builder()
                .sessionId("s1")
                .conversationId("conv-1")
                .rollbackCount(3)
                .build();

        assertEquals("s1", data.getSessionId());
        assertEquals("conv-1", data.getConversationId());
        assertEquals(3, data.getRollbackCount());
    }
}
