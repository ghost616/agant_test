package com.ghost616.agentinteg.tool;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendResultToParentToolTest {

    @Mock
    private AgentExecutionContext ctx;

    private SendResultToParentTool tool;

    @BeforeEach
    void setUp() {
        tool = new SendResultToParentTool(SendResultToParentTool.createToolConfig());
    }

    @Test
    void createToolConfig_返回CUSTOM类型且工具名为send_result_to_parent() {
        ToolConfigDTO config = SendResultToParentTool.createToolConfig();

        assertEquals(ToolType.CUSTOM, config.getToolType());
        assertEquals("send_result_to_parent", config.getName());
        assertNull(config.getId());
        assertNotNull(config.getDescription());
        assertFalse(config.getDescription().isBlank());
        assertTrue(config.getParameterSchema().contains("\"result\""));
        assertTrue(config.getParameterSchema().contains("\"required\""));
    }

    @Test
    void execute_正常路径_发送结果到父会话() throws Exception {
        String arguments = """
                {
                    "result": "子会话执行完成"
                }
                """;

        when(ctx.getParentSessionId()).thenReturn("parent-1");
        when(ctx.getModelId()).thenReturn("100");

        String result = tool.execute(ctx, arguments);

        assertTrue(result.contains("\"status\":\"success\""));
        assertTrue(result.contains("已发送执行结果到父会话"));
        verify(ctx).sendUserMessage("parent-1", "子会话执行完成", "100", null);
    }

    @Test
    void execute_无父会话ID_返回错误JSON() throws Exception {
        String arguments = """
                {
                    "result": "子会话执行结果"
                }
                """;

        when(ctx.getParentSessionId()).thenReturn(null);

        String result = tool.execute(ctx, arguments);

        assertTrue(result.contains("\"status\":\"error\""));
        assertTrue(result.contains("当前会话不是子会话，无法发送结果到父会话"));
        verify(ctx, never()).sendUserMessage(anyString(), anyString(), anyString(), any());
    }

    @Test
    void execute_缺少result参数_返回错误JSON() throws Exception {
        String arguments = "{}";

        String result = tool.execute(ctx, arguments);

        assertTrue(result.contains("\"status\":\"error\""));
        assertTrue(result.contains("缺少 result 参数"));
        verify(ctx, never()).sendUserMessage(anyString(), anyString(), anyString(), any());
    }

    @Test
    void execute_JSON解析异常_返回错误JSON() {
        String invalidJson = "{invalid}";

        String result = tool.execute(ctx, invalidJson);

        assertTrue(result.contains("\"status\":\"error\""));
        verify(ctx, never()).sendUserMessage(anyString(), anyString(), anyString(), any());
    }
}
