package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

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
    }

    @Test
    void builder应正确设置继承与自有字段() {
        AgentExecutionContext context = new AgentExecutionContext(
                "s1", "agent-1", "sys_prompt", "model-1", null,
                new ArrayList<>(), new ArrayList<>(), null,
                new AgentExecutionContext.AgentContextMutator(),
                new HashMap<>(), new HashMap<>(), null, "", null, null);
        MessageRollbackLogData data = MessageRollbackLogData.builder()
                .context(context)
                .sessionId("s1")
                .rollbackCount(3)
                .build();

        assertSame(context, data.getContext());
        assertEquals("s1", data.getSessionId());
        assertEquals(3, data.getRollbackCount());
    }
}
