package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseQueryProvider;
import com.ghost616.agentinteg.knowledge.TextChunkWithFile;
import com.ghost616.agentinteg.knowledge.TextChunkWithFile.TextChunk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeSearchToolTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private KnowledgeBaseQueryProvider provider;

    @Mock
    private AgentExecutionContext ctx;

    private KnowledgeSearchTool tool;

    @BeforeEach
    void setUp() {
        tool = new KnowledgeSearchTool(provider);
    }

    @Test
    void getToolName_返回kb_search() {
        assertEquals("kb_search", tool.getToolName());
    }

    @Test
    void getDescription_返回非空描述() {
        assertNotNull(tool.getDescription());
        assertFalse(tool.getDescription().isBlank());
    }

    @Test
    void getParameterSchema_包含必填参数() {
        String schema = tool.getParameterSchema();
        assertTrue(schema.contains("\"knowledgeBaseId\""));
        assertTrue(schema.contains("\"searchType\""));
        assertTrue(schema.contains("\"query\""));
        assertTrue(schema.contains("\"required\""));
    }

    @Test
    void execute_正常路径_返回合并后的文本块() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "searchType": "vector",
                  "query": "hello",
                  "searchLimit": 5,
                  "contextLines": 3
                }
                """;
        TextChunkWithFile withFile = new TextChunkWithFile(
                2L, "a.txt", List.of(new TextChunk(5, "line5"), new TextChunk(6, "line6"), new TextChunk(8, "line8")));
        when(provider.searchChunks(100L, null, "vector", "hello", 5, 3)).thenReturn(List.of(withFile));

        String result = tool.execute(ctx, arguments);
        JsonNode root = MAPPER.readTree(result);

        assertTrue(root.isArray());
        assertEquals(1, root.size());
        JsonNode file = root.get(0);
        assertEquals(2, file.get("fileId").asLong());
        assertEquals("a.txt", file.get("fileName").asText());
        assertEquals(2, file.get("chunkList").size());
        assertEquals(5, file.get("chunkList").get(0).get("lineNumber").asInt());
        assertEquals("line5\nline6", file.get("chunkList").get(0).get("text").asText());
        assertEquals(8, file.get("chunkList").get(1).get("lineNumber").asInt());
        assertEquals("line8", file.get("chunkList").get(1).get("text").asText());
        verify(provider).searchChunks(100L, null, "vector", "hello", 5, 3);
    }

    @Test
    void execute_未传searchLimit和contextLines时使用默认值() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "searchType": "full_text",
                  "query": "world"
                }
                """;
        when(provider.searchChunks(100L, null, "full_text", "world", 10, 3)).thenReturn(List.of());

        String result = tool.execute(ctx, arguments);

        assertNotNull(result);
        verify(provider).searchChunks(100L, null, "full_text", "world", 10, 3);
    }

    @Test
    void execute_传fileId时透传给provider() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 7,
                  "searchType": "vector",
                  "query": "test"
                }
                """;
        when(provider.searchChunks(100L, 7L, "vector", "test", 10, 3)).thenReturn(List.of());

        tool.execute(ctx, arguments);

        verify(provider).searchChunks(100L, 7L, "vector", "test", 10, 3);
    }

    @Test
    void execute_缺少knowledgeBaseId_返回错误() throws Exception {
        String result = tool.execute(ctx, "{\"searchType\":\"vector\",\"query\":\"hello\"}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("knowledgeBaseId"));
        verify(provider, never()).searchChunks(anyLong(), any(), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void execute_缺少searchType或query_返回错误() throws Exception {
        String result1 = tool.execute(ctx, "{\"knowledgeBaseId\":100,\"query\":\"hello\"}");
        String result2 = tool.execute(ctx, "{\"knowledgeBaseId\":100,\"searchType\":\"vector\"}");

        assertTrue(result1.contains("error"));
        assertTrue(result2.contains("error"));
        verify(provider, never()).searchChunks(anyLong(), any(), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void execute_provider抛出异常_返回错误JSON() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "searchType": "vector",
                  "query": "hello"
                }
                """;
        when(provider.searchChunks(100L, null, "vector", "hello", 10, 3))
                .thenThrow(new RuntimeException("搜索失败"));

        String result = tool.execute(ctx, arguments);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("搜索失败"));
    }

    @Test
    void execute_provider异常消息含特殊字符_返回合法JSON() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "searchType": "vector",
                  "query": "hello"
                }
                """;
        String specialMsg = "搜索失败: \"引号\" \n 第二行 \\ 反斜杠";
        when(provider.searchChunks(100L, null, "vector", "hello", 10, 3))
                .thenThrow(new RuntimeException(specialMsg));

        String result = tool.execute(ctx, arguments);

        JsonNode root = assertDoesNotThrow(() -> MAPPER.readTree(result));
        assertEquals("error", root.get("status").asText());
        assertTrue(root.get("errMsg").asText().contains("引号"));
        assertTrue(root.get("errMsg").asText().contains("第二行"));
    }
}
