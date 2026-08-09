package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogLevel;
import com.ghost616.agentbase.enums.LogType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogDataTest {

    private static class StubLogData extends LogData {
        @Override
        public LogType logType() {
            return null;
        }
    }

    @Test
    void logLevel默认应为null() {
        LogData data = new StubLogData();
        assertNull(data.getLogLevel());
    }

    @Test
    void setLogLevel后getLogLevel应返回设置值() {
        LogData data = new StubLogData();
        data.setLogLevel(LogLevel.INFO);
        assertEquals(LogLevel.INFO, data.getLogLevel());
    }

    @Test
    void logType为抽象方法应可被覆写() {
        LogData data = new StubLogData();
        assertNull(data.logType());
    }
}
