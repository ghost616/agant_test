package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
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
        tool = new KnowledgeFileChunkTool(KnowledgeFileChunkTool.createToolConfig(), provider);
    }

    @Test
    void createToolConfig_返回CUSTOM类型且name为default_tool_rag_file_chunk() {
        ToolConfigDTO config = KnowledgeFileChunkTool.createToolConfig();
        assertNull(config.getId());
        assertEquals(ToolType.CUSTOM, config.getToolType());
        assertEquals("default_tool_rag_file_chunk", config.getName());
    }

    @Test
    void createToolConfig_包含描述和参数schema() {
        ToolConfigDTO config = KnowledgeFileChunkTool.createToolConfig();
        assertNotNull(config.getDescription());
        assertFalse(config.getDescription().isBlank());
        assertNotNull(config.getParameterSchema());
        assertTrue(config.getParameterSchema().contains("\"knowledgeBaseId\""));
        assertTrue(config.getParameterSchema().contains("\"fileId\""));
        assertTrue(config.getParameterSchema().contains("\"required\""));
    }

    @Test
    void execute_正常路径_按行号合并chunks返回纯文本() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 2,
                  "startLine": 0,
                  "endLine": 10
                }
                """;
        TextChunkWithFile withFile = new TextChunkWithFile(
                "100", "2", "a.txt", List.of(new TextChunk(0, "line0"), new TextChunk(1, "line1")));
        when(provider.getFileChunks("100", "2", 0, 10)).thenReturn(withFile);

        String result = tool.execute(ctx, arguments);

        assertEquals("line0\nline1", result);
        verify(provider).getFileChunks("100", "2", 0, 10);
    }

    @Test
    void execute_非连续行号_按行号升序合并() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 2,
                  "startLine": 0,
                  "endLine": 10
                }
                """;
        TextChunkWithFile withFile = new TextChunkWithFile(
                "100", "2", "a.txt", List.of(new TextChunk(3, "line3"), new TextChunk(1, "line1")));
        when(provider.getFileChunks("100", "2", 0, 10)).thenReturn(withFile);

        String result = tool.execute(ctx, arguments);

        assertEquals("line1\nline3", result);
    }

    @Test
    void execute_provider返回null_返回空字符串() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 2,
                  "startLine": 0,
                  "endLine": 10
                }
                """;
        when(provider.getFileChunks("100", "2", 0, 10)).thenReturn(null);

        String result = tool.execute(ctx, arguments);

        assertEquals("", result);
    }

    @Test
    void execute_chunks为空_返回空字符串() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 2,
                  "startLine": 0,
                  "endLine": 10
                }
                """;
        when(provider.getFileChunks("100", "2", 0, 10)).thenReturn(
                new TextChunkWithFile("100", "2", "a.txt", List.of()));

        String result = tool.execute(ctx, arguments);

        assertEquals("", result);
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
        when(provider.getFileChunks("100", "2", 0, 50)).thenReturn(
                new TextChunkWithFile("100", "2", "a.txt", List.of()));

        tool.execute(ctx, arguments);

        verify(provider).getFileChunks("100", "2", 0, 50);
    }

    @Test
    void execute_未传endLine时按文件最大行数解析() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 2
                }
                """;
        FileInfo file = new FileInfo("2", "a.txt", "文档", 120);
        when(provider.searchFiles("100", null, 1000)).thenReturn(List.of(file));
        when(provider.getFileChunks("100", "2", 0, 120)).thenReturn(
                new TextChunkWithFile("100", "2", "a.txt", List.of()));

        tool.execute(ctx, arguments);

        verify(provider).searchFiles("100", null, 1000);
        verify(provider).getFileChunks("100", "2", 0, 120);
    }

    @Test
    void execute_未传endLine且找不到文件时使用IntegerMAX_VALUE() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 999
                }
                """;
        when(provider.searchFiles("100", null, 1000)).thenReturn(List.of());
        when(provider.getFileChunks("100", "999", 0, Integer.MAX_VALUE)).thenReturn(
                new TextChunkWithFile("100", "999", "x.txt", List.of()));

        tool.execute(ctx, arguments);

        verify(provider).getFileChunks("100", "999", 0, Integer.MAX_VALUE);
    }

    @Test
    void execute_缺少knowledgeBaseId或fileId_返回错误() throws Exception {
        String result1 = tool.execute(ctx, "{\"fileId\":2}");
        String result2 = tool.execute(ctx, "{\"knowledgeBaseId\":100}");

        assertTrue(result1.contains("error"));
        assertTrue(result2.contains("error"));
        verify(provider, never()).getFileChunks(anyString(), anyString(), anyInt(), anyInt());
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
        when(provider.getFileChunks("100", "2", 0, 10)).thenThrow(new RuntimeException("查询失败"));

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
        when(provider.getFileChunks("100", "2", 0, 10)).thenThrow(new RuntimeException(specialMsg));

        String result = tool.execute(ctx, arguments);

        JsonNode root = assertDoesNotThrow(() -> MAPPER.readTree(result));
        assertEquals("error", root.get("status").asText());
        assertTrue(root.get("errMsg").asText().contains("引号"));
        assertTrue(root.get("errMsg").asText().contains("第二行"));
    }
}
