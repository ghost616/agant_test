package com.ghost616.agentbase.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogLevelTest {

    @Test
    void INFO枚举应存在() {
        assertNotNull(LogLevel.INFO);
    }

    @Test
    void ERROR枚举应存在() {
        assertNotNull(LogLevel.ERROR);
    }

    @Test
    void INFO的getCode应返回INFO() {
        assertEquals("INFO", LogLevel.INFO.getCode());
    }

    @Test
    void INFO的getDescription应返回信息() {
        assertEquals("信息", LogLevel.INFO.getDescription());
    }

    @Test
    void ERROR的getCode应返回ERROR() {
        assertEquals("ERROR", LogLevel.ERROR.getCode());
    }

    @Test
    void ERROR的getDescription应返回错误() {
        assertEquals("错误", LogLevel.ERROR.getDescription());
    }

    @Test
    void 枚举值数量应为2() {
        assertEquals(2, LogLevel.values().length);
    }

    @Test
    void 按名称valueOf应能解析() {
        assertEquals(LogLevel.INFO, LogLevel.valueOf("INFO"));
        assertEquals(LogLevel.ERROR, LogLevel.valueOf("ERROR"));
    }
}
