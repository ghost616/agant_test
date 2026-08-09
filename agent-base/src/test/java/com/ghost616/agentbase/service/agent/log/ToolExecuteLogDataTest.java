package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class ToolExecuteLogDataTest {

    @Test
    void logType应返回TOOL_EXECUTE() {
        ToolExecuteLogData data = ToolExecuteLogData.builder().build();
        assertEquals(LogType.TOOL_EXECUTE, data.logType());
    }

    @Test
    void builder应正确设置继承与自有字段() {
        AgentExecutionContext context = new AgentExecutionContext(
                "s1", "agent-1", "sys_prompt", "model-1", null,
                new ArrayList<>(), new ArrayList<>(), null,
                new AgentExecutionContext.AgentContextMutator(),
                new HashMap<>(), new HashMap<>(), null, "", null, null);
        ToolExecuteLogData data = ToolExecuteLogData.builder()
                .context(context)
                .sessionId("s1")
                .toolCallId("tc1")
                .toolCallName("getWeather")
                .toolCallArguments("{\"loc\":\"Beijing\"}")
                .toolType("regular")
                .queueStatus("executing")
                .build();

        assertSame(context, data.getContext());
        assertEquals("s1", data.getSessionId());
        assertEquals("tc1", data.getToolCallId());
        assertEquals("getWeather", data.getToolCallName());
        assertEquals("{\"loc\":\"Beijing\"}", data.getToolCallArguments());
        assertEquals("regular", data.getToolType());
        assertEquals("executing", data.getQueueStatus());
    }
}
