package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefreshLogDataTest {

    @Test
    void logType应返回REFRESH() {
        RefreshLogData data = RefreshLogData.builder().build();
        assertEquals(LogType.REFRESH, data.logType());
    }

    @Test
    void 字段应默认为null() {
        RefreshLogData data = RefreshLogData.builder().build();
        assertNull(data.getContext());
        assertNull(data.getLogLevel());
        assertNull(data.getSessionId());
        assertNull(data.getRefreshTarget());
    }

    @Test
    void builder应正确设置继承与自有字段() {
        RefreshLogData data = RefreshLogData.builder()
                .sessionId("s1")
                .refreshTarget("HISTORY")
                .build();

        assertEquals("s1", data.getSessionId());
        assertEquals("HISTORY", data.getRefreshTarget());
    }

    @Test
    void 不同刷新目标应可设置() {
        assertEquals("SESSION_VARIABLES", RefreshLogData.builder()
                .sessionId("s1").refreshTarget("SESSION_VARIABLES").build().getRefreshTarget());
        assertEquals("CONVERSATION_VARIABLES", RefreshLogData.builder()
                .sessionId("s1").refreshTarget("CONVERSATION_VARIABLES").build().getRefreshTarget());
        assertEquals("CHILD_SESSIONS", RefreshLogData.builder()
                .sessionId("s1").refreshTarget("CHILD_SESSIONS").build().getRefreshTarget());
    }
}
