package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class StreamEventLogDataTest {

    @Test
    void logType应返回STREAM_EVENT() {
        StreamEventLogData data = StreamEventLogData.builder().build();
        assertEquals(LogType.STREAM_EVENT, data.logType());
    }

    @Test
    void 字段应默认为null() {
        StreamEventLogData data = StreamEventLogData.builder().build();
        assertNull(data.getContext());
        assertNull(data.getLogLevel());
        assertNull(data.getEventType());
        assertNull(data.getHasToolCalls());
    }

    @Test
    void builder应正确设置继承与自有字段() {
        AgentExecutionContext context = new AgentExecutionContext(
                "s1", "agent-1", "sys_prompt", "model-1", null,
                new ArrayList<>(), new ArrayList<>(), null,
                new AgentExecutionContext.AgentContextMutator(),
                new HashMap<>(), new HashMap<>(), null, "", null, null);
        StreamEventLogData data = StreamEventLogData.builder()
                .context(context)
                .eventType("ToolCallDetected")
                .hasToolCalls(true)
                .build();

        assertSame(context, data.getContext());
        assertEquals("ToolCallDetected", data.getEventType());
        assertTrue(data.getHasToolCalls());
    }

    @Test
    void hasToolCalls应支持false值() {
        StreamEventLogData data = StreamEventLogData.builder()
                .hasToolCalls(false)
                .build();
        assertFalse(data.getHasToolCalls());
    }
}
