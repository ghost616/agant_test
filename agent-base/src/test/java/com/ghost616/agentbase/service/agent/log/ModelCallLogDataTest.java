package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelCallLogDataTest {

    @Test
    void logType应返回MODEL_CALL() {
        ModelCallLogData data = ModelCallLogData.builder().build();
        assertEquals(LogType.MODEL_CALL, data.logType());
    }

    @Test
    void 数字字段应默认为0() {
        ModelCallLogData data = ModelCallLogData.builder().build();
        assertEquals(0, data.getMessageCount());
        assertEquals(0, data.getToolCount());
        assertNull(data.getToolNames());
        assertNull(data.getThinking());
    }

    @Test
    void builder应正确设置继承与自有字段() {
        AgentExecutionContext context = new AgentExecutionContext(
                "s1", "agent-1", "sys_prompt", "model-1", null,
                new ArrayList<>(), new ArrayList<>(), null,
                new AgentExecutionContext.AgentContextMutator(),
                new HashMap<>(), new HashMap<>(), null, "", null, null);
        List<String> toolNames = new ArrayList<>(List.of("get_weather", "search"));
        ModelCallLogData data = ModelCallLogData.builder()
                .context(context)
                .messageCount(5)
                .toolCount(3)
                .toolNames(toolNames)
                .thinking(true)
                .build();

        assertSame(context, data.getContext());
        assertEquals(5, data.getMessageCount());
        assertEquals(3, data.getToolCount());
        assertEquals(toolNames, data.getToolNames());
        assertTrue(data.getThinking());
    }
}
