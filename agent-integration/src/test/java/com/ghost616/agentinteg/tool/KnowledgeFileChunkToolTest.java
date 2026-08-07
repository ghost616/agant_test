package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentinteg.knowledge.FileInfo;
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
class KnowledgeFileChunkToolTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private KnowledgeBaseQueryProvider provider;

    @Mock
    private AgentExecutionContext ctx;

    private KnowledgeFileChunkTool tool;

    @BeforeEach
    void setUp() {
        tool = new KnowledgeFileChunkTool(provider);
    }

    @Test
    void getToolName_返回kb_file_chunk() {
        assertEquals("kb_file_chunk", tool.getToolName());
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
        assertTrue(schema.contains("\"fileId\""));
        assertTrue(schema.contains("\"required\""));
    }

    @Test
    void execute_正常路径_返回文本块() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 2,
                  "startLine": 0,
                  "endLine": 10
                }
                """;
        TextChunkWithFile withFile = new TextChunkWithFile(
                2L, "a.txt", List.of(new TextChunk(0, "line0"), new TextChunk(1, "line1")));
        when(provider.getFileChunks(100L, 2L, 0, 10)).thenReturn(withFile);

        String result = tool.execute(ctx, arguments);
        JsonNode root = MAPPER.readTree(result);

        assertEquals(2, root.get("fileId").asLong());
        assertEquals("a.txt", root.get("fileName").asText());
        assertEquals(2, root.get("chunkList").size());
        verify(provider).getFileChunks(100L, 2L, 0, 10);
    }

    @Test
    void execute_未传startLine时使用默认值0() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 2,
                  "endLine": 50
                }
                """;
        when(provider.getFileChunks(100L, 2L, 0, 50)).thenReturn(
                new TextChunkWithFile(2L, "a.txt", List.of()));

        tool.execute(ctx, arguments);

        verify(provider).getFileChunks(100L, 2L, 0, 50);
    }

    @Test
    void execute_未传endLine时按文件最大行数解析() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 2
                }
                """;
        FileInfo file = new FileInfo(2L, "a.txt", "文档", 120);
        when(provider.searchFiles(100L, null, 1000)).thenReturn(List.of(file));
        when(provider.getFileChunks(100L, 2L, 0, 120)).thenReturn(
                new TextChunkWithFile(2L, "a.txt", List.of()));

        tool.execute(ctx, arguments);

        verify(provider).searchFiles(100L, null, 1000);
        verify(provider).getFileChunks(100L, 2L, 0, 120);
    }

    @Test
    void execute_未传endLine且找不到文件时使用IntegerMAX_VALUE() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 999
                }
                """;
        when(provider.searchFiles(100L, null, 1000)).thenReturn(List.of());
        when(provider.getFileChunks(100L, 999L, 0, Integer.MAX_VALUE)).thenReturn(
                new TextChunkWithFile(999L, "x.txt", List.of()));

        tool.execute(ctx, arguments);

        verify(provider).getFileChunks(100L, 999L, 0, Integer.MAX_VALUE);
    }

    @Test
    void execute_缺少knowledgeBaseId或fileId_返回错误() throws Exception {
        String result1 = tool.execute(ctx, "{\"fileId\":2}");
        String result2 = tool.execute(ctx, "{\"knowledgeBaseId\":100}");

        assertTrue(result1.contains("error"));
        assertTrue(result2.contains("error"));
        verify(provider, never()).getFileChunks(anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void execute_provider抛出异常_返回错误JSON() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 2,
                  "startLine": 0,
                  "endLine": 10
                }
                """;
        when(provider.getFileChunks(100L, 2L, 0, 10)).thenThrow(new RuntimeException("查询失败"));

        String result = tool.execute(ctx, arguments);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("查询失败"));
    }

    @Test
    void execute_provider异常消息含特殊字符_返回合法JSON() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 2,
                  "startLine": 0,
                  "endLine": 10
                }
                """;
        String specialMsg = "查询失败: \"引号\" \n 第二行 \\ 反斜杠";
        when(provider.getFileChunks(100L, 2L, 0, 10)).thenThrow(new RuntimeException(specialMsg));

        String result = tool.execute(ctx, arguments);

        JsonNode root = assertDoesNotThrow(() -> MAPPER.readTree(result));
        assertEquals("error", root.get("status").asText());
        assertTrue(root.get("errMsg").asText().contains("引号"));
        assertTrue(root.get("errMsg").asText().contains("第二行"));
    }
}
