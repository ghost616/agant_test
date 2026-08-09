package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class RouteLogDataTest {

    @Test
    void logType应返回ROUTE() {
        RouteLogData data = RouteLogData.builder().build();
        assertEquals(LogType.ROUTE, data.logType());
    }

    @Test
    void 字段应默认为null() {
        RouteLogData data = RouteLogData.builder().build();
        assertNull(data.getContext());
        assertNull(data.getLogLevel());
        assertNull(data.getRequestType());
    }

    @Test
    void builder应正确设置继承与自有字段() {
        AgentExecutionContext context = new AgentExecutionContext(
                "s1", "agent-1", "sys_prompt", "model-1", null,
                new ArrayList<>(), new ArrayList<>(), null,
                new AgentExecutionContext.AgentContextMutator(),
                new HashMap<>(), new HashMap<>(), null, "", null, null);
        RouteLogData data = RouteLogData.builder()
                .context(context)
                .requestType("completions")
                .build();

        assertSame(context, data.getContext());
        assertEquals("completions", data.getRequestType());
    }
}
