package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseInfo;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseQueryProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseInfoToolTest {

    @Mock
    private KnowledgeBaseQueryProvider provider;

    @Mock
    private AgentExecutionContext ctx;

    private KnowledgeBaseInfoTool tool;

    @BeforeEach
    void setUp() {
        tool = new KnowledgeBaseInfoTool(provider);
    }

    @Test
    void getToolName_返回kb_info() {
        assertEquals("kb_info", tool.getToolName());
    }

    @Test
    void getDescription_返回非空描述() {
        assertNotNull(tool.getDescription());
        assertFalse(tool.getDescription().isBlank());
    }

    @Test
    void getParameterSchema_包含必填参数sessionId() {
        String schema = tool.getParameterSchema();
        assertTrue(schema.contains("\"sessionId\""));
        assertTrue(schema.contains("\"required\""));
    }

    @Test
    void execute_正常路径_返回知识库信息() throws Exception {
        String arguments = "{\"sessionId\": \"s1\"}";
        KnowledgeBaseInfo info = new KnowledgeBaseInfo(1L, "kb-name", "kb-desc");
        when(provider.getKnowledgeBaseInfo("s1")).thenReturn(info);

        String result = tool.execute(ctx, arguments);

        assertTrue(result.contains("\"kbId\":1"));
        assertTrue(result.contains("\"kbName\":\"kb-name\""));
        assertTrue(result.contains("\"kbDescription\":\"kb-desc\""));
        verify(provider).getKnowledgeBaseInfo("s1");
    }

    @Test
    void execute_缺少sessionId_返回错误() throws Exception {
        String result = tool.execute(ctx, "{}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("sessionId"));
        verify(provider, never()).getKnowledgeBaseInfo(anyString());
    }

    @Test
    void execute_provider抛出异常_返回错误JSON() throws Exception {
        String arguments = "{\"sessionId\": \"s1\"}";
        when(provider.getKnowledgeBaseInfo("s1")).thenThrow(new RuntimeException("查询失败"));

        String result = tool.execute(ctx, arguments);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("查询失败"));
    }

    @Test
    void execute_provider异常消息含特殊字符_返回合法JSON() throws Exception {
        String arguments = "{\"sessionId\": \"s1\"}";
        String specialMsg = "查询失败: \"引号\" \n 第二行 \\ 反斜杠";
        when(provider.getKnowledgeBaseInfo("s1")).thenThrow(new RuntimeException(specialMsg));

        String result = tool.execute(ctx, arguments);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = assertDoesNotThrow(() -> mapper.readTree(result));
        assertEquals("error", root.get("status").asText());
        assertTrue(root.get("errMsg").asText().contains("引号"));
        assertTrue(root.get("errMsg").asText().contains("第二行"));
    }
}
