package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentinteg.history.HistoryMessageItem;
import com.ghost616.agentinteg.history.HistoryMessageQueryProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryQueryToolTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private HistoryMessageQueryProvider provider;

    @Mock
    private AgentExecutionContext ctx;

    private HistoryQueryTool tool;

    @BeforeEach
    void setUp() {
        tool = new HistoryQueryTool(HistoryQueryTool.createToolConfig(), provider);
    }

    @Test
    void createToolConfig_返回CUSTOM类型且name为default_tool_history_query() {
        ToolConfigDTO config = HistoryQueryTool.createToolConfig();
        assertNull(config.getId());
        assertEquals(ToolType.CUSTOM, config.getToolType());
        assertEquals("default_tool_history_query", config.getName());
    }

    @Test
    void createToolConfig_包含描述和参数schema() {
        ToolConfigDTO config = HistoryQueryTool.createToolConfig();
        assertNotNull(config.getDescription());
        assertFalse(config.getDescription().isBlank());
        assertNotNull(config.getParameterSchema());
        String schema = config.getParameterSchema();
        assertTrue(schema.contains("\"seqs\""));
        assertTrue(schema.contains("\"includeReasoning\""));
        assertTrue(schema.contains("\"required\""));
    }

    @Test
    void execute_正常路径_调用provider并返回消息列表() throws Exception {
        String arguments = """
                {
                  "seqs": [1, 3, 5],
                  "includeReasoning": true
                }
                """;
        when(ctx.getSessionId()).thenReturn("42");
        HistoryMessageItem m1 = new HistoryMessageItem(
                "user", "你好", null, null, null);
        HistoryMessageItem m2 = new HistoryMessageItem(
                "assistant", "你好！", "思考过程", null, null);
        HistoryMessageItem m3 = new HistoryMessageItem(
                "assistant", null, "推理",
                List.of(new HistoryMessageItem.HistoryToolCallItem(
                        "call-1", "getWeather", "{\"loc\":\"Beijing\"}")),
                null);
        HistoryMessageItem m4 = new HistoryMessageItem(
                "tool", "{\"temp\":25}", null, null,
                new HistoryMessageItem.HistoryToolResultItem("call-1", "getWeather"));
        when(provider.getMessagesBySeqs("42", List.of(1, 3, 5), true))
                .thenReturn(List.of(m1, m2, m3, m4));

        String result = tool.execute(ctx, arguments);
        JsonNode root = MAPPER.readTree(result);

        assertFalse(root.has("status"));
        JsonNode messages = root.get("messages");
        assertEquals(4, messages.size());

        JsonNode first = messages.get(0);
        assertEquals("user", first.get("role").asText());
        assertEquals("你好", first.get("content").asText());
        assertFalse(first.has("reasoning"));
        assertFalse(first.has("toolCalls"));
        assertFalse(first.has("toolResult"));

        JsonNode second = messages.get(1);
        assertEquals("assistant", second.get("role").asText());
        assertEquals("你好！", second.get("content").asText());
        assertEquals("思考过程", second.get("reasoning").asText());

        JsonNode third = messages.get(2);
        assertEquals("assistant", third.get("role").asText());
        assertEquals(1, third.get("toolCalls").size());
        assertEquals("call-1", third.get("toolCalls").get(0).get("toolCallId").asText());
        assertEquals("getWeather", third.get("toolCalls").get(0).get("toolCallName").asText());
        assertEquals("{\"loc\":\"Beijing\"}", third.get("toolCalls").get(0).get("toolCallArguments").asText());

        JsonNode fourth = messages.get(3);
        assertEquals("tool", fourth.get("role").asText());
        assertEquals("{\"temp\":25}", fourth.get("content").asText());
        assertEquals("call-1", fourth.get("toolResult").get("toolCallId").asText());
        assertEquals("getWeather", fourth.get("toolResult").get("toolCallName").asText());
        assertEquals("{\"temp\":25}", fourth.get("toolResult").get("content").asText());

        verify(provider).getMessagesBySeqs("42", List.of(1, 3, 5), true);
    }

    @Test
    void execute_includeReasoning为false_不返回reasoning() throws Exception {
        String arguments = """
                {
                  "seqs": [2]
                }
                """;
        when(ctx.getSessionId()).thenReturn("42");
        HistoryMessageItem m = new HistoryMessageItem(
                "assistant", "回复", "隐藏的推理", null, null);
        when(provider.getMessagesBySeqs("42", List.of(2), false))
                .thenReturn(List.of(m));

        String result = tool.execute(ctx, arguments);
        JsonNode root = MAPPER.readTree(result);

        JsonNode message = root.get("messages").get(0);
        assertEquals("assistant", message.get("role").asText());
        assertEquals("回复", message.get("content").asText());
        assertFalse(message.has("reasoning"));
        verify(provider).getMessagesBySeqs("42", List.of(2), false);
    }

    @Test
    void execute_无匹配消息_返回空列表非错误() throws Exception {
        String arguments = """
                {
                  "seqs": [99]
                }
                """;
        when(ctx.getSessionId()).thenReturn("42");
        when(provider.getMessagesBySeqs("42", List.of(99), false))
                .thenReturn(List.of());

        String result = tool.execute(ctx, arguments);
        JsonNode root = MAPPER.readTree(result);

        assertFalse(root.has("status"));
        assertEquals(0, root.get("messages").size());
    }

    @Test
    void execute_provider返回null_返回空列表() throws Exception {
        String arguments = """
                {
                  "seqs": [1]
                }
                """;
        when(ctx.getSessionId()).thenReturn("42");
        when(provider.getMessagesBySeqs("42", List.of(1), false))
                .thenReturn(null);

        String result = tool.execute(ctx, arguments);
        JsonNode root = MAPPER.readTree(result);

        assertFalse(root.has("status"));
        assertEquals(0, root.get("messages").size());
    }

    @Test
    void execute_缺少seqs_返回错误() throws Exception {
        when(ctx.getSessionId()).thenReturn("42");
        String result = tool.execute(ctx, "{\"includeReasoning\":false}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("seqs"));
        verify(provider, never()).getMessagesBySeqs(anyString(), any(), anyBoolean());
    }

    @Test
    void execute_seqs为空数组_返回错误() throws Exception {
        when(ctx.getSessionId()).thenReturn("42");
        String result = tool.execute(ctx, "{\"seqs\":[]}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("seqs"));
        verify(provider, never()).getMessagesBySeqs(anyString(), any(), anyBoolean());
    }

    @Test
    void execute_seqs含非整数_返回错误() throws Exception {
        when(ctx.getSessionId()).thenReturn("42");
        String result = tool.execute(ctx, "{\"seqs\":[1,\"abc\"]}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("seqs"));
        verify(provider, never()).getMessagesBySeqs(anyString(), any(), anyBoolean());
    }

    @Test
    void execute_无法获取会话ID_返回错误() throws Exception {
        when(ctx.getSessionId()).thenReturn(null);
        String result = tool.execute(ctx, "{\"seqs\":[1]}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("会话"));
        verify(provider, never()).getMessagesBySeqs(anyString(), any(), anyBoolean());
    }

    @Test
    void execute_provider抛出异常_返回错误JSON() throws Exception {
        String arguments = """
                {
                  "seqs": [1]
                }
                """;
        when(ctx.getSessionId()).thenReturn("42");
        when(provider.getMessagesBySeqs("42", List.of(1), false))
                .thenThrow(new RuntimeException("历史消息查询失败"));

        String result = tool.execute(ctx, arguments);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("历史消息查询失败"));
    }
}
