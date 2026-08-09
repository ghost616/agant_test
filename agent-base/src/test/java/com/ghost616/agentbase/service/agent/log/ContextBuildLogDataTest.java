package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContextBuildLogDataTest {

    @Test
    void logType应返回CONTEXT_BUILD() {
        ContextBuildLogData data = ContextBuildLogData.builder().build();
        assertEquals(LogType.CONTEXT_BUILD, data.logType());
    }

    @Test
    void 字段应默认为null或false() {
        ContextBuildLogData data = ContextBuildLogData.builder().build();
        assertNull(data.getLogLevel());
        assertNull(data.getSessionId());
        assertNull(data.getAgentId());
        assertNull(data.getModelId());
        assertEquals(0, data.getToolCount());
        assertEquals(0, data.getHistoryCount());
        assertFalse(data.isSubSession());
        assertFalse(data.isCacheHit());
        assertNull(data.getSessionVariables());
    }

    @Test
    void builder应正确设置字段() {
        Map<String, String> vars = new HashMap<>();
        vars.put("k1", "v1");
        ContextBuildLogData data = ContextBuildLogData.builder()
                .sessionId("s1")
                .agentId("agent-1")
                .modelId("model-1")
                .toolCount(3)
                .historyCount(5)
                .isSubSession(true)
                .cacheHit(true)
                .sessionVariables(vars)
                .build();

        assertEquals("s1", data.getSessionId());
        assertEquals("agent-1", data.getAgentId());
        assertEquals("model-1", data.getModelId());
        assertEquals(3, data.getToolCount());
        assertEquals(5, data.getHistoryCount());
        assertTrue(data.isSubSession());
        assertTrue(data.isCacheHit());
        assertEquals(vars, data.getSessionVariables());
    }

    @Test
    void 不应继承ContextLogData无context字段() {
        ContextBuildLogData data = ContextBuildLogData.builder().build();
        assertFalse(ContextLogData.class.isAssignableFrom(data.getClass()));
    }
}
