package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequestEntryLogDataTest {

    @Test
    void logType应返回REQUEST_ENTRY() {
        RequestEntryLogData data = RequestEntryLogData.builder().build();
        assertEquals(LogType.REQUEST_ENTRY, data.logType());
    }

    @Test
    void 字段应默认为null() {
        RequestEntryLogData data = RequestEntryLogData.builder().build();
        assertNull(data.getContext());
        assertNull(data.getModelId());
        assertNull(data.getConversationId());
        assertNull(data.getContent());
        assertNull(data.getIsToolContinue());
    }

    @Test
    void builder应正确设置继承与自有字段() {
        AgentExecutionContext context = new AgentExecutionContext(
                "s1", "agent-1", "sys_prompt", "model-1", null,
                new java.util.ArrayList<>(), new java.util.ArrayList<>(), null,
                new AgentExecutionContext.AgentContextMutator(),
                new java.util.HashMap<>(), new java.util.HashMap<>(), null, "", null, null);
        RequestEntryLogData data = RequestEntryLogData.builder()
                .context(context)
                .modelId("model-1")
                .conversationId("conv-1")
                .content("hello")
                .isToolContinue(true)
                .build();

        assertSame(context, data.getContext());
        assertEquals("s1", data.getContext().getSessionId());
        assertEquals("model-1", data.getModelId());
        assertEquals("conv-1", data.getConversationId());
        assertEquals("hello", data.getContent());
        assertTrue(data.getIsToolContinue());
    }

    @Test
    void isToolContinue应支持false值() {
        RequestEntryLogData data = RequestEntryLogData.builder()
                .isToolContinue(false)
                .build();
        assertFalse(data.getIsToolContinue());
    }
}
