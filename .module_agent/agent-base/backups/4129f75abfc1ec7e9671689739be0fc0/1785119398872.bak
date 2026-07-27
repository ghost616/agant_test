package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionVariableSystemToolTest {

    @Mock
    private AgentExecutionContext ctx;

    private SessionVariableSystemTool tool;

    @BeforeEach
    void setUp() {
        tool = new SessionVariableSystemTool();
    }

    @Test
    void getToolName_returns_session_variable() {
        assertEquals("session_variable", tool.getToolName());
    }

    @Test
    void getDescription_returns_non_null() {
        assertNotNull(tool.getDescription());
    }

    @Test
    void getParameterSchema_returns_valid_json() {
        String schema = tool.getParameterSchema();
        assertNotNull(schema);
        assertTrue(schema.contains("\"action\""));
        assertTrue(schema.contains("\"key\""));
        assertTrue(schema.contains("\"value\""));
    }

    @Test
    void add_operation_calls_putSessionVariable_and_returns_ok() {
        String result = tool.execute(ctx, "{\"action\":\"add\",\"key\":\"myKey\",\"value\":\"myValue\"}");

        verify(ctx).putSessionVariable("myKey", "myValue");
        assertTrue(result.contains("\"status\":\"ok\""));
        assertTrue(result.contains("\"action\":\"add\""));
        assertTrue(result.contains("\"key\":\"myKey\""));
    }

    @Test
    void get_operation_calls_getSessionVariable_and_returns_value() {
        when(ctx.getSessionVariable("myKey")).thenReturn("storedValue");

        String result = tool.execute(ctx, "{\"action\":\"get\",\"key\":\"myKey\"}");

        verify(ctx).getSessionVariable("myKey");
        assertTrue(result.contains("\"status\":\"ok\""));
        assertTrue(result.contains("\"value\":\"storedValue\""));
    }

    @Test
    void get_operation_with_null_value_returns_null() {
        when(ctx.getSessionVariable("missingKey")).thenReturn(null);

        String result = tool.execute(ctx, "{\"action\":\"get\",\"key\":\"missingKey\"}");

        assertTrue(result.contains("\"value\":null"));
    }

    @Test
    void remove_operation_calls_removeSessionVariable_and_returns_ok() {
        String result = tool.execute(ctx, "{\"action\":\"remove\",\"key\":\"myKey\"}");

        verify(ctx).removeSessionVariable("myKey");
        assertTrue(result.contains("\"status\":\"ok\""));
        assertTrue(result.contains("\"action\":\"remove\""));
    }

    @Test
    void missing_action_returns_error() {
        String result = tool.execute(ctx, "{\"key\":\"myKey\"}");

        verifyNoInteractions(ctx);
        assertTrue(result.contains("\"error\""));
        assertTrue(result.contains("缺少 action 或 key 参数"));
    }

    @Test
    void missing_key_returns_error() {
        String result = tool.execute(ctx, "{\"action\":\"add\"}");

        verifyNoInteractions(ctx);
        assertTrue(result.contains("\"error\""));
        assertTrue(result.contains("缺少 action 或 key 参数"));
    }

    @Test
    void add_operation_missing_value_returns_error() {
        String result = tool.execute(ctx, "{\"action\":\"add\",\"key\":\"myKey\"}");

        verifyNoMoreInteractions(ctx);
        assertTrue(result.contains("\"error\""));
        assertTrue(result.contains("add 操作缺少 value 参数"));
    }

    @Test
    void invalid_action_returns_error() {
        String result = tool.execute(ctx, "{\"action\":\"invalid\",\"key\":\"myKey\"}");

        verifyNoInteractions(ctx);
        assertTrue(result.contains("\"error\""));
        assertTrue(result.contains("不支持的 action"));
    }

    @Test
    void invalid_json_returns_error() {
        String result = tool.execute(ctx, "not valid json");

        verifyNoInteractions(ctx);
        assertTrue(result.contains("\"error\""));
    }

    @Test
    void blank_key_returns_error() {
        String result = tool.execute(ctx, "{\"action\":\"add\",\"key\":\"  \",\"value\":\"val\"}");

        verifyNoInteractions(ctx);
        assertTrue(result.contains("\"error\""));
        assertTrue(result.contains("缺少 action 或 key 参数"));
    }
}
