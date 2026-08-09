package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheRemoveLogDataTest {

    @Test
    void logType应返回CACHE_REMOVE() {
        CacheRemoveLogData data = CacheRemoveLogData.builder().build();
        assertEquals(LogType.CACHE_REMOVE, data.logType());
    }

    @Test
    void 字段应默认为null() {
        CacheRemoveLogData data = CacheRemoveLogData.builder().build();
        assertNull(data.getContext());
        assertNull(data.getLogLevel());
        assertNull(data.getSessionId());
    }

    @Test
    void builder应正确设置继承与自有字段() {
        CacheRemoveLogData data = CacheRemoveLogData.builder()
                .sessionId("s1")
                .build();

        assertEquals("s1", data.getSessionId());
    }
}
