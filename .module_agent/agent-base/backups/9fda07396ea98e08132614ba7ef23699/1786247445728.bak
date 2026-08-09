package com.ghost616.agentbase.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogTypeTest {

    @Test
    void 枚举应可被加载() {
        assertNotNull(LogType.class);
    }

    @Test
    void 枚举值数量应为2() {
        assertEquals(2, LogType.values().length);
    }

    @Test
    void 应包含请求入口与调用来源枚举值() {
        assertNotNull(LogType.REQUEST_ENTRY);
        assertNotNull(LogType.CALL_SOURCE);
    }

    @Test
    void REQUEST_ENTRY的code与description应正确() {
        assertEquals("REQUEST_ENTRY", LogType.REQUEST_ENTRY.getCode());
        assertEquals("请求入口", LogType.REQUEST_ENTRY.getDescription());
    }

    @Test
    void CALL_SOURCE的code与description应正确() {
        assertEquals("CALL_SOURCE", LogType.CALL_SOURCE.getCode());
        assertEquals("调用来源", LogType.CALL_SOURCE.getDescription());
    }
}
