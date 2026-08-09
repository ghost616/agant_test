package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogLevel;
import com.ghost616.agentbase.enums.LogType;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogDataTest {

    @SuperBuilder
    private static class StubLogData extends LogData {
        @Override
        public LogType logType() {
            return null;
        }
    }

    @Test
    void logLevel默认应为null() {
        LogData data = StubLogData.builder().build();
        assertNull(data.getLogLevel());
    }

    @Test
    void logLevel应可通过builder设置() {
        LogData data = StubLogData.builder().logLevel(LogLevel.INFO).build();
        assertEquals(LogLevel.INFO, data.getLogLevel());
    }

    @Test
    void logType为抽象方法应可被覆写() {
        LogData data = StubLogData.builder().build();
        assertNull(data.logType());
    }
}
