package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentinteg.knowledge.FileInfo;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseQueryProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeFileInfoToolTest {

    @Mock
    private KnowledgeBaseQueryProvider provider;

    @Mock
    private AgentExecutionContext ctx;

    private KnowledgeFileInfoTool tool;

    @BeforeEach
    void setUp() {
        tool = new KnowledgeFileInfoTool(KnowledgeFileInfoTool.createToolConfig(), provider);
    }

    @Test
    void createToolConfig_返回CUSTOM类型且name为default_tool_rag_file_info() {
        ToolConfigDTO config = KnowledgeFileInfoTool.createToolConfig();
        assertNull(config.getId());
        assertEquals(ToolType.CUSTOM, config.getToolType());
        assertEquals("default_tool_rag_file_info", config.getName());
    }

    @Test
    void createToolConfig_包含描述和参数schema() {
        ToolConfigDTO config = KnowledgeFileInfoTool.createToolConfig();
        assertNotNull(config.getDescription());
        assertFalse(config.getDescription().isBlank());
        assertNotNull(config.getParameterSchema());
        assertTrue(config.getParameterSchema().contains("\"knowledgeBaseId\""));
        assertTrue(config.getParameterSchema().contains("\"fileId\""));
        assertTrue(config.getParameterSchema().contains("\"required\""));
    }

    @Test
    void execute_正常路径_返回文件列表() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileName": "readme",
                  "searchLimit": 5
                }
                """;
        FileInfo file = new FileInfo("2", "readme.md", "说明文档", 120);
        when(provider.searchFiles("100", "readme", 5)).thenReturn(List.of(file));

        String result = tool.execute(ctx, arguments);

        assertTrue(result.contains("\"fileId\":\"2\""));
        assertTrue(result.contains("\"fileName\":\"readme.md\""));
        assertTrue(result.contains("\"maxLineCount\":120"));
        verify(provider).searchFiles("100", "readme", 5);
    }

    @Test
    void execute_传fileId时过滤返回对应文件() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 2
                }
                """;
        FileInfo file2 = new FileInfo("2", "b.txt", "文档2", 50);
        FileInfo file3 = new FileInfo("3", "c.txt", "文档3", 80);
        when(provider.searchFiles("100", null, 10)).thenReturn(List.of(file2, file3));

        String result = tool.execute(ctx, arguments);

        assertTrue(result.contains("\"fileId\":\"2\""));
        assertFalse(result.contains("\"fileId\":\"3\""));
        assertFalse(result.contains("c.txt"));
        verify(provider).searchFiles("100", null, 10);
    }

    @Test
    void execute_传fileId但无匹配_返回空列表() throws Exception {
        String arguments = """
                {
                  "knowledgeBaseId": 100,
                  "fileId": 99
                }
                """;
        when(provider.searchFiles("100", null, 10)).thenReturn(List.of(
                new FileInfo("2", "b.txt", "文档2", 50)));

        String result = tool.execute(ctx, arguments);

        JsonNode root = new ObjectMapper().readTree(result);
        assertTrue(root.isArray());
        assertEquals(0, root.size());
    }

    @Test
    void execute_未传searchLimit时使用默认值10() throws Exception {
        String arguments = "{\"knowledgeBaseId\": 100}";
        when(provider.searchFiles("100", null, 10)).thenReturn(List.of());

        String result = tool.execute(ctx, arguments);

        assertNotNull(result);
        verify(provider).searchFiles("100", null, 10);
    }

    @Test
    void execute_缺少knowledgeBaseId_返回错误() throws Exception {
        String result = tool.execute(ctx, "{}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("knowledgeBaseId"));
        verify(provider, never()).searchFiles(anyString(), anyString(), anyInt());
    }

    @Test
    void execute_provider抛出异常_返回错误JSON() throws Exception {
        String arguments = "{\"knowledgeBaseId\": 100}";
        when(provider.searchFiles(eq("100"), isNull(), eq(10))).thenThrow(new RuntimeException("搜索失败"));

        String result = tool.execute(ctx, arguments);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("搜索失败"));
    }

    @Test
    void execute_provider异常消息含特殊字符_返回合法JSON() throws Exception {
        String arguments = "{\"knowledgeBaseId\": 100}";
        String specialMsg = "搜索失败: \"引号\" \n 第二行 \\ 反斜杠";
        when(provider.searchFiles(eq("100"), isNull(), eq(10))).thenThrow(new RuntimeException(specialMsg));

        String result = tool.execute(ctx, arguments);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = assertDoesNotThrow(() -> mapper.readTree(result));
        assertEquals("error", root.get("status").asText());
        assertTrue(root.get("errMsg").asText().contains("引号"));
        assertTrue(root.get("errMsg").asText().contains("第二行"));
    }
}
