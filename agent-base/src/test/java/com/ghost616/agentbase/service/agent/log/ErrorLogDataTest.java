package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class ErrorLogDataTest {

    @Test
    void logType应返回ERROR_LOG() {
        ErrorLogData data = ErrorLogData.builder().build();
        assertEquals(LogType.ERROR_LOG, data.logType());
    }

    @Test
    void 字段应默认为null() {
        ErrorLogData data = ErrorLogData.builder().build();
        assertNull(data.getContext());
        assertNull(data.getLogLevel());
        assertNull(data.getErrorCode());
        assertNull(data.getMessage());
        assertNull(data.getException());
    }

    @Test
    void builder应正确设置继承与自有字段() {
        AgentExecutionContext context = new AgentExecutionContext(
                "s1", "agent-1", "sys_prompt", "model-1", null,
                new ArrayList<>(), new ArrayList<>(), null,
                new AgentExecutionContext.AgentContextMutator(),
                new HashMap<>(), new HashMap<>(), null, "", null, null);
        RuntimeException exception = new RuntimeException("boom");
        ErrorLogData data = ErrorLogData.builder()
                .context(context)
                .errorCode("SYS-001")
                .message("出错")
                .exception(exception)
                .build();

        assertSame(context, data.getContext());
        assertEquals("SYS-001", data.getErrorCode());
        assertEquals("出错", data.getMessage());
        assertSame(exception, data.getException());
    }
}
