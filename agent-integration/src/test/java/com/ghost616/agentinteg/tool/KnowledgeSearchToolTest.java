package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseQueryProvider;
import com.ghost616.agentinteg.knowledge.SearchType;
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
        tool = new KnowledgeSearchTool(KnowledgeSearchTool.createToolConfig(), provider);
    }

    @Test
    void createToolConfig_返回CUSTOM类型且name为default_tool_rag_search() {
        ToolConfigDTO config = KnowledgeSearchTool.createToolConfig();
        assertNull(config.getId());
        assertEquals(ToolType.CUSTOM, config.getToolType());
        assertEquals("default_tool_rag_search", config.getName());
    }

    @Test
    void createToolConfig_包含描述和参数schema() {
        ToolConfigDTO config = KnowledgeSearchTool.createToolConfig();
        assertNotNull(config.getDescription());
        assertFalse(config.getDescription().isBlank());
        assertNotNull(config.getParameterSchema());
        String schema = config.getParameterSchema();
        assertTrue(schema.contains("\"knowledgeBaseId\""));
        assertTrue(schema.contains("\"searchType\""));
        assertTrue(schema.contains("\"enum\""));
        assertTrue(schema.contains("\"VECTOR\""));
        assertTrue(schema.contains("\"FULLTEXT\""));
        assertTrue(schema.contains("\"HYBRID\""));
        assertTrue(schema.contains("\"query\""));
        assertTrue(schema.contains("\"required\""));
    }

    @Test
    void execute_正常路径_扩大行范围并调用getFileChunks返回合并块() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "searchType": "VECTOR",
                  "query": "hello",
                  "searchLimit": 5,
                  "contextLines": 3
                }
                """;
        TextChunkWithFile withFile = new TextChunkWithFile(
                100L, 2L, "a.txt", List.of(new TextChunk(5, "line5"), new TextChunk(6, "line6"), new TextChunk(8, "line8")));
        when(provider.searchChunks(100L, null, SearchType.VECTOR, "hello", 5)).thenReturn(List.of(withFile));
        TextChunkWithFile context = new TextChunkWithFile(100L, 2L, "a.txt",
                List.of(new TextChunk(5, "line5"), new TextChunk(6, "line6"),
                        new TextChunk(7, "line7"), new TextChunk(8, "line8")));
        when(provider.getFileChunks(100L, 2L, 2, 11)).thenReturn(context);

        String result = tool.execute(ctx, arguments);
        JsonNode root = MAPPER.readTree(result);

        assertTrue(root.isArray());
        assertEquals(1, root.size());
        JsonNode file = root.get(0);
        assertEquals(100, file.get("knowledgeBaseId").asLong());
        assertEquals(2, file.get("fileId").asLong());
        assertFalse(file.has("fileName"));
        assertEquals(1, file.get("chunks").size());
        assertEquals(5, file.get("chunks").get(0).get("lineNumber").asInt());
        assertEquals("line5\nline6\nline7\nline8", file.get("chunks").get(0).get("text").asText());
        verify(provider).searchChunks(100L, null, SearchType.VECTOR, "hello", 5);
        verify(provider).getFileChunks(100L, 2L, 2, 11);
    }

    @Test
    void execute_同一文件多个withFile_按文件分组合并输出() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "searchType": "VECTOR",
                  "query": "hello"
                }
                """;
        TextChunkWithFile withFile1 = new TextChunkWithFile(
                100L, 2L, "a.txt", List.of(new TextChunk(5, "line5"), new TextChunk(6, "line6")));
        TextChunkWithFile withFile2 = new TextChunkWithFile(
                100L, 2L, "a.txt", List.of(new TextChunk(7, "line7")));
        TextChunkWithFile withFile3 = new TextChunkWithFile(
                100L, 3L, "b.txt", List.of(new TextChunk(1, "b1")));
        when(provider.searchChunks(100L, null, SearchType.VECTOR, "hello", 10))
                .thenReturn(List.of(withFile1, withFile2, withFile3));
        when(provider.getFileChunks(100L, 2L, 2, 9)).thenReturn(new TextChunkWithFile(100L, 2L, "a.txt",
                List.of(new TextChunk(5, "line5"), new TextChunk(6, "line6"), new TextChunk(7, "line7"))));
        when(provider.getFileChunks(100L, 2L, 4, 10)).thenReturn(new TextChunkWithFile(100L, 2L, "a.txt",
                List.of(new TextChunk(7, "line7"))));
        when(provider.getFileChunks(100L, 3L, 1, 4)).thenReturn(new TextChunkWithFile(100L, 3L, "b.txt",
                List.of(new TextChunk(1, "b1"))));

        String result = tool.execute(ctx, arguments);
        JsonNode root = MAPPER.readTree(result);

        assertEquals(2, root.size());
        JsonNode file2 = root.get(0);
        assertEquals(100, file2.get("knowledgeBaseId").asLong());
        assertEquals(2, file2.get("fileId").asLong());
        assertEquals(1, file2.get("chunks").size());
        assertEquals(5, file2.get("chunks").get(0).get("lineNumber").asInt());
        assertEquals("line5\nline6\nline7", file2.get("chunks").get(0).get("text").asText());
        JsonNode file3 = root.get(1);
        assertEquals(3, file3.get("fileId").asLong());
        assertEquals(1, file3.get("chunks").size());
        verify(provider).getFileChunks(100L, 2L, 2, 9);
        verify(provider).getFileChunks(100L, 2L, 4, 10);
        verify(provider).getFileChunks(100L, 3L, 1, 4);
    }

    @Test
    void execute_同一文件重复行号_去重后合并() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "searchType": "VECTOR",
                  "query": "hello"
                }
                """;
        TextChunkWithFile withFile1 = new TextChunkWithFile(
                100L, 2L, "a.txt", List.of(new TextChunk(5, "line5"), new TextChunk(6, "first6")));
        TextChunkWithFile withFile2 = new TextChunkWithFile(
                100L, 2L, "a.txt", List.of(new TextChunk(6, "second6"), new TextChunk(7, "line7")));
        when(provider.searchChunks(100L, null, SearchType.VECTOR, "hello", 10))
                .thenReturn(List.of(withFile1, withFile2));
        when(provider.getFileChunks(100L, 2L, 2, 9)).thenReturn(new TextChunkWithFile(100L, 2L, "a.txt",
                List.of(new TextChunk(5, "line5"), new TextChunk(6, "first6"), new TextChunk(7, "line7"))));
        when(provider.getFileChunks(100L, 2L, 3, 10)).thenReturn(new TextChunkWithFile(100L, 2L, "a.txt",
                List.of(new TextChunk(6, "second6"), new TextChunk(7, "line7"))));

        String result = tool.execute(ctx, arguments);
        JsonNode root = MAPPER.readTree(result);

        assertEquals(1, root.size());
        JsonNode file = root.get(0);
        assertEquals(1, file.get("chunks").size());
        assertEquals(5, file.get("chunks").get(0).get("lineNumber").asInt());
        assertEquals("line5\nfirst6\nline7", file.get("chunks").get(0).get("text").asText());
        verify(provider).getFileChunks(100L, 2L, 2, 9);
        verify(provider).getFileChunks(100L, 2L, 3, 10);
    }

    @Test
    void execute_getFileChunks返回null_跳过该范围() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "searchType": "VECTOR",
                  "query": "hello"
                }
                """;
        TextChunkWithFile withFile = new TextChunkWithFile(
                100L, 2L, "a.txt", List.of(new TextChunk(5, "line5")));
        when(provider.searchChunks(100L, null, SearchType.VECTOR, "hello", 10)).thenReturn(List.of(withFile));
        when(provider.getFileChunks(100L, 2L, 2, 8)).thenReturn(null);

        String result = tool.execute(ctx, arguments);
        JsonNode root = MAPPER.readTree(result);

        assertTrue(root.isArray());
        assertEquals(1, root.size());
        assertEquals(0, root.get(0).get("chunks").size());
        verify(provider).getFileChunks(100L, 2L, 2, 8);
    }

    @Test
    void execute_searchType为FULLTEXT小写_转枚举调用provider() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "searchType": "fulltext",
                  "query": "world"
                }
                """;
        when(provider.searchChunks(100L, null, SearchType.FULLTEXT, "world", 10)).thenReturn(List.of());

        String result = tool.execute(ctx, arguments);

        assertNotNull(result);
        verify(provider).searchChunks(100L, null, SearchType.FULLTEXT, "world", 10);
        verify(provider, never()).getFileChunks(anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void execute_未传searchLimit和contextLines时使用默认值() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "searchType": "FULLTEXT",
                  "query": "world"
                }
                """;
        when(provider.searchChunks(100L, null, SearchType.FULLTEXT, "world", 10)).thenReturn(List.of());

        String result = tool.execute(ctx, arguments);

        assertNotNull(result);
        verify(provider).searchChunks(100L, null, SearchType.FULLTEXT, "world", 10);
    }

    @Test
    void execute_传fileId时透传给provider() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 7,
                  "searchType": "HYBRID",
                  "query": "test"
                }
                """;
        when(provider.searchChunks(100L, 7L, SearchType.HYBRID, "test", 10)).thenReturn(List.of());

        tool.execute(ctx, arguments);

        verify(provider).searchChunks(100L, 7L, SearchType.HYBRID, "test", 10);
    }

    @Test
    void execute_缺少knowledgeBaseId_返回错误() throws Exception {
        String result = tool.execute(ctx, "{\"searchType\":\"VECTOR\",\"query\":\"hello\"}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("knowledgeBaseId"));
        verify(provider, never()).searchChunks(anyLong(), any(), any(), anyString(), anyInt());
    }

    @Test
    void execute_缺少searchType或query_返回错误() throws Exception {
        String result1 = tool.execute(ctx, "{\"knowledgeBaseId\":100,\"query\":\"hello\"}");
        String result2 = tool.execute(ctx, "{\"knowledgeBaseId\":100,\"searchType\":\"VECTOR\"}");

        assertTrue(result1.contains("error"));
        assertTrue(result2.contains("error"));
        verify(provider, never()).searchChunks(anyLong(), any(), any(), anyString(), anyInt());
    }

    @Test
    void execute_无效searchType_返回错误() throws Exception {
        String result = tool.execute(ctx, "{\"knowledgeBaseId\":100,\"searchType\":\"UNKNOWN\",\"query\":\"hello\"}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("searchType"));
        verify(provider, never()).searchChunks(anyLong(), any(), any(), anyString(), anyInt());
    }

    @Test
    void execute_provider抛出异常_返回错误JSON() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "searchType": "VECTOR",
                  "query": "hello"
                }
                """;
        when(provider.searchChunks(100L, null, SearchType.VECTOR, "hello", 10))
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
                  "searchType": "VECTOR",
                  "query": "hello"
                }
                """;
        String specialMsg = "搜索失败: \"引号\" \n 第二行 \\ 反斜杠";
        when(provider.searchChunks(100L, null, SearchType.VECTOR, "hello", 10))
                .thenThrow(new RuntimeException(specialMsg));

        String result = tool.execute(ctx, arguments);

        JsonNode root = assertDoesNotThrow(() -> MAPPER.readTree(result));
        assertEquals("error", root.get("status").asText());
        assertTrue(root.get("errMsg").asText().contains("引号"));
        assertTrue(root.get("errMsg").asText().contains("第二行"));
    }
}
