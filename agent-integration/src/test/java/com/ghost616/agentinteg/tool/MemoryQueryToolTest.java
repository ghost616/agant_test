package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentinteg.knowledge.SearchType;
import com.ghost616.agentinteg.memory.MemoryQueryProvider;
import com.ghost616.agentinteg.memory.MemoryResult;
import com.ghost616.agentinteg.memory.MessageSeqByRole;
import com.ghost616.agentinteg.memory.SeqRange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryQueryToolTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private MemoryQueryProvider provider;

    @Mock
    private AgentExecutionContext ctx;

    private MemoryQueryTool tool;

    @BeforeEach
    void setUp() {
        tool = new MemoryQueryTool(MemoryQueryTool.createToolConfig(), provider);
    }

    @Test
    void createToolConfig_返回CUSTOM类型且name为default_tool_memory_search() {
        ToolConfigDTO config = MemoryQueryTool.createToolConfig();
        assertNull(config.getId());
        assertEquals(ToolType.CUSTOM, config.getToolType());
        assertEquals("default_tool_memory_search", config.getName());
    }

    @Test
    void createToolConfig_包含描述和参数schema() {
        ToolConfigDTO config = MemoryQueryTool.createToolConfig();
        assertNotNull(config.getDescription());
        assertFalse(config.getDescription().isBlank());
        assertNotNull(config.getParameterSchema());
        String schema = config.getParameterSchema();
        assertTrue(schema.contains("\"query\""));
        assertTrue(schema.contains("\"searchType\""));
        assertTrue(schema.contains("\"enum\""));
        assertTrue(schema.contains("\"VECTOR\""));
        assertTrue(schema.contains("\"FULLTEXT\""));
        assertTrue(schema.contains("\"HYBRID\""));
        assertTrue(schema.contains("\"memoryType\""));
        assertTrue(schema.contains("\"GROUP\""));
        assertTrue(schema.contains("\"DAILY\""));
        assertTrue(schema.contains("\"startTime\""));
        assertTrue(schema.contains("\"endTime\""));
        assertTrue(schema.contains("\"required\""));
    }

    @Test
    void execute_正常路径_调用provider并汇总序号列表() throws Exception {
        String arguments = """
                {
                  "query": "喜欢吃什么",
                  "searchType": "VECTOR",
                  "memoryType": "GROUP",
                  "startTime": 1700000000000,
                  "endTime": 1700000000999
                }
                """;
        when(ctx.getSessionId()).thenReturn("42");
        MemoryResult r1 = new MemoryResult("喜欢苹果", 1, 3, "GROUP");
        MemoryResult r2 = new MemoryResult("喜欢香蕉", 5, 6, "GROUP");
        when(provider.getMemories("42", SearchType.VECTOR, "GROUP", 1700000000000L, 1700000000999L, "喜欢吃什么"))
                .thenReturn(List.of(r1, r2));
        when(provider.getMessageSeqsByRole("42", List.of(new SeqRange(1, 3), new SeqRange(5, 6))))
                .thenReturn(new MessageSeqByRole(List.of(1, 5), List.of(2, 6), List.of(3)));

        String result = tool.execute(ctx, arguments);
        JsonNode root = MAPPER.readTree(result);

        assertFalse(root.has("status"));
        JsonNode results = root.get("results");
        assertEquals(2, results.size());
        assertEquals("喜欢苹果", results.get(0).get("content").asText());
        assertEquals(1, results.get(0).get("startSeq").asInt());
        assertEquals(3, results.get(0).get("endSeq").asInt());
        assertEquals("GROUP", results.get(0).get("memoryType").asText());
        assertEquals(List.of(1, 5), toIntList(root.get("userSeqList")));
        assertEquals(List.of(2, 6), toIntList(root.get("toolSeqList")));
        assertEquals(List.of(3), toIntList(root.get("assistantSeqList")));
        verify(provider).getMemories("42", SearchType.VECTOR, "GROUP", 1700000000000L, 1700000000999L, "喜欢吃什么");
        verify(provider).getMessageSeqsByRole("42", List.of(new SeqRange(1, 3), new SeqRange(5, 6)));
    }

    @Test
    void execute_未传memoryType和时间_透传null() throws Exception {
        String arguments = """
                {
                  "query": "hello",
                  "searchType": "fulltext"
                }
                """;
        when(ctx.getSessionId()).thenReturn("42");
        when(provider.getMemories("42", SearchType.FULLTEXT, null, null, null, "hello"))
                .thenReturn(List.of());

        String result = tool.execute(ctx, arguments);

        assertNotNull(result);
        verify(provider).getMemories("42", SearchType.FULLTEXT, null, null, null, "hello");
        verify(provider, never()).getMessageSeqsByRole(anyString(), anyList());
    }

    @Test
    void execute_无匹配结果_返回空列表非错误() throws Exception {
        String arguments = """
                {
                  "query": "nothing",
                  "searchType": "HYBRID"
                }
                """;
        when(ctx.getSessionId()).thenReturn("42");
        when(provider.getMemories("42", SearchType.HYBRID, null, null, null, "nothing"))
                .thenReturn(List.of());

        String result = tool.execute(ctx, arguments);
        JsonNode root = MAPPER.readTree(result);

        assertFalse(root.has("status"));
        assertEquals(0, root.get("results").size());
        assertEquals(0, root.get("userSeqList").size());
        assertEquals(0, root.get("toolSeqList").size());
        assertEquals(0, root.get("assistantSeqList").size());
    }

    @Test
    void execute_getMessageSeqsByRole返回null_跳过该结果() throws Exception {
        String arguments = """
                {
                  "query": "hello",
                  "searchType": "VECTOR"
                }
                """;
        when(ctx.getSessionId()).thenReturn("42");
        MemoryResult r1 = new MemoryResult("记忆1", 1, 2, "GROUP");
        when(provider.getMemories("42", SearchType.VECTOR, null, null, null, "hello"))
                .thenReturn(List.of(r1));
        when(provider.getMessageSeqsByRole("42", List.of(new SeqRange(1, 2)))).thenReturn(null);

        String result = tool.execute(ctx, arguments);
        JsonNode root = MAPPER.readTree(result);

        assertEquals(1, root.get("results").size());
        assertEquals(0, root.get("userSeqList").size());
        assertEquals(0, root.get("toolSeqList").size());
        assertEquals(0, root.get("assistantSeqList").size());
    }

    @Test
    void execute_批量调用返回的序号_合并去重() throws Exception {
        String arguments = """
                {
                  "query": "hello",
                  "searchType": "VECTOR"
                }
                """;
        when(ctx.getSessionId()).thenReturn("42");
        MemoryResult r1 = new MemoryResult("记忆1", 1, 3, "GROUP");
        MemoryResult r2 = new MemoryResult("记忆2", 3, 5, "DAILY");
        when(provider.getMemories("42", SearchType.VECTOR, null, null, null, "hello"))
                .thenReturn(List.of(r1, r2));
        when(provider.getMessageSeqsByRole("42", List.of(new SeqRange(1, 3), new SeqRange(3, 5))))
                .thenReturn(new MessageSeqByRole(
                        List.of(1, 2, 3, 3, 4), List.of(2, 2, 5), List.of(1, 3, 3, 5)));

        String result = tool.execute(ctx, arguments);
        JsonNode root = MAPPER.readTree(result);

        assertEquals(2, root.get("results").size());
        assertEquals(List.of(1, 2, 3, 4), toIntList(root.get("userSeqList")));
        assertEquals(List.of(2, 5), toIntList(root.get("toolSeqList")));
        assertEquals(List.of(1, 3, 5), toIntList(root.get("assistantSeqList")));
        verify(provider).getMessageSeqsByRole("42", List.of(new SeqRange(1, 3), new SeqRange(3, 5)));
    }

    @Test
    void execute_缺少query_返回错误() throws Exception {
        when(ctx.getSessionId()).thenReturn("42");
        String result = tool.execute(ctx, "{\"searchType\":\"VECTOR\"}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("query"));
        verify(provider, never()).getMemories(anyString(), any(), any(), any(), any(), anyString());
    }

    @Test
    void execute_缺少searchType_返回错误() throws Exception {
        when(ctx.getSessionId()).thenReturn("42");
        String result = tool.execute(ctx, "{\"query\":\"hello\"}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("searchType"));
        verify(provider, never()).getMemories(anyString(), any(), any(), any(), any(), anyString());
    }

    @Test
    void execute_无效searchType_返回错误() throws Exception {
        when(ctx.getSessionId()).thenReturn("42");
        String result = tool.execute(ctx, "{\"query\":\"hello\",\"searchType\":\"UNKNOWN\"}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("searchType"));
        verify(provider, never()).getMemories(anyString(), any(), any(), any(), any(), anyString());
    }

    @Test
    void execute_无效memoryType_返回错误() throws Exception {
        when(ctx.getSessionId()).thenReturn("42");
        String result = tool.execute(ctx, "{\"query\":\"hello\",\"searchType\":\"VECTOR\",\"memoryType\":\"WEEKLY\"}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("memoryType"));
        verify(provider, never()).getMemories(anyString(), any(), any(), any(), any(), anyString());
    }

    @Test
    void execute_无效startTime_返回错误() throws Exception {
        when(ctx.getSessionId()).thenReturn("42");
        String result = tool.execute(ctx, "{\"query\":\"hello\",\"searchType\":\"VECTOR\",\"startTime\":\"abc\"}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("startTime"));
        verify(provider, never()).getMemories(anyString(), any(), any(), any(), any(), anyString());
    }

    @Test
    void execute_无效endTime_返回错误() throws Exception {
        when(ctx.getSessionId()).thenReturn("42");
        String result = tool.execute(ctx, "{\"query\":\"hello\",\"searchType\":\"VECTOR\",\"endTime\":\"abc\"}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("endTime"));
        verify(provider, never()).getMemories(anyString(), any(), any(), any(), any(), anyString());
    }

    @Test
    void execute_无法获取会话ID_返回错误() throws Exception {
        when(ctx.getSessionId()).thenReturn(null);
        String result = tool.execute(ctx, "{\"query\":\"hello\",\"searchType\":\"VECTOR\"}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("会话"));
        verify(provider, never()).getMemories(anyString(), any(), any(), any(), any(), anyString());
    }

    @Test
    void execute_provider抛出异常_返回错误JSON() throws Exception {
        String arguments = """
                {
                  "query": "hello",
                  "searchType": "VECTOR"
                }
                """;
        when(ctx.getSessionId()).thenReturn("42");
        when(provider.getMemories("42", SearchType.VECTOR, null, null, null, "hello"))
                .thenThrow(new RuntimeException("记忆查询失败"));

        String result = tool.execute(ctx, arguments);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("记忆查询失败"));
    }

    private List<Integer> toIntList(JsonNode node) {
        List<Integer> list = new java.util.ArrayList<>();
        for (JsonNode item : node) {
            list.add(item.asInt());
        }
        return list;
    }
}
