package com.ghost616.platform.systemtest;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemTestSubSessionToolTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AgentExecutionContext ctx;

    private SystemTestSubSessionTool tool;

    @BeforeEach
    void setUp() {
        tool = new SystemTestSubSessionTool();
    }

    // === 正向：正常执行 ===
    @Test
    void 正常执行返回role和content() throws Exception {
        when(ctx.getModelId()).thenReturn(1L);
        when(ctx.createChildSession(anyString(), anyString(), anyLong(), isNull(), isNull(), isNull()))
                .thenReturn(100L);

        Message reply = Message.builder().role("assistant").content("Hello from child").build();
        when(ctx.sendUserMessage(eq(100L), eq("Hi"), eq(1L))).thenReturn(reply);

        String result = tool.execute(ctx, "{\"sessionName\":\"test-session\",\"message\":\"Hi\"}");

        assertTrue(result.contains("\"role\":\"assistant\""));
        assertTrue(result.contains("\"content\":\"Hello from child\""));
    }

    // === 反向：createChildSession 返回 null ===
    @Test
    void createChildSession返回null返回错误() {
        when(ctx.getModelId()).thenReturn(1L);
        when(ctx.createChildSession(anyString(), anyString(), anyLong(), isNull(), isNull(), isNull()))
                .thenReturn(null);

        String result = tool.execute(ctx, "{\"sessionName\":\"test\",\"message\":\"hello\"}");

        assertEquals("{\"error\":\"createChildSession returned null\"}", result);
    }

    // === 反向：JSON 解析异常 ===
    @Test
    void JSON解析异常返回错误信息() {
        String result = tool.execute(ctx, "invalid json");

        assertTrue(result.contains("\"error\""));
        assertTrue(result.contains("Unrecognized token"));
    }

    // === 反向：其他运行时异常 ===
    @Test
    void createChildSession抛出异常时返回错误信息() {
        when(ctx.getModelId()).thenThrow(new RuntimeException("DB connection failed"));

        String result = tool.execute(ctx, "{\"sessionName\":\"test\",\"message\":\"hello\"}");

        assertTrue(result.contains("\"error\""));
        assertTrue(result.contains("DB connection failed"));
    }

    // === 边界：sessionName 为空 ===
    @Test
    void sessionName为空字符串仍然正常执行() {
        when(ctx.getModelId()).thenReturn(1L);
        when(ctx.createChildSession(anyString(), anyString(), anyLong(), isNull(), isNull(), isNull()))
                .thenReturn(200L);

        Message reply = Message.builder().role("user").content("ok").build();
        when(ctx.sendUserMessage(eq(200L), eq("msg"), eq(1L))).thenReturn(reply);

        String result = tool.execute(ctx, "{\"sessionName\":\"\",\"message\":\"msg\"}");

        assertTrue(result.contains("\"role\":\"user\""));
        assertTrue(result.contains("\"content\":\"ok\""));
    }

    // === 边界：message 内容超长 ===
    @Test
    void message内容超长仍然正常执行() {
        String longMsg = "a".repeat(10000);
        when(ctx.getModelId()).thenReturn(1L);
        when(ctx.createChildSession(anyString(), anyString(), anyLong(), isNull(), isNull(), isNull()))
                .thenReturn(300L);

        Message reply = Message.builder().role("assistant").content(longMsg).build();
        when(ctx.sendUserMessage(eq(300L), eq(longMsg), eq(1L))).thenReturn(reply);

        String result = tool.execute(ctx, "{\"sessionName\":\"long\",\"message\":\"" + longMsg + "\"}");

        assertTrue(result.contains("\"role\":\"assistant\""));
        assertTrue(result.length() > 10000);
        assertTrue(result.contains("\"content\":\""));
    }
}
