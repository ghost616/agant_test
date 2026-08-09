package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolContinueLogDataTest {

    @Test
    void logType应返回TOOL_CONTINUE() {
        ToolContinueLogData data = ToolContinueLogData.builder().build();
        assertEquals(LogType.TOOL_CONTINUE, data.logType());
    }

    @Test
    void 数字字段应默认为0() {
        ToolContinueLogData data = ToolContinueLogData.builder().build();
        assertEquals(0, data.getResultCount());
        assertNull(data.getToolNames());
        assertNull(data.getSessionId());
    }

    @Test
    void builder应正确设置继承与自有字段() {
        AgentExecutionContext context = new AgentExecutionContext(
                "s1", "agent-1", "sys_prompt", "model-1", null,
                new ArrayList<>(), new ArrayList<>(), null,
                new AgentExecutionContext.AgentContextMutator(),
                new HashMap<>(), new HashMap<>(), null, "", null, null);
        List<String> toolNames = new ArrayList<>(List.of("getWeather", "getTime"));
        ToolContinueLogData data = ToolContinueLogData.builder()
                .context(context)
                .sessionId("s1")
                .resultCount(2)
                .toolNames(toolNames)
                .build();

        assertSame(context, data.getContext());
        assertEquals("s1", data.getSessionId());
        assertEquals(2, data.getResultCount());
        assertEquals(toolNames, data.getToolNames());
    }
}
