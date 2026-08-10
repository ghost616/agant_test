package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseInfo;
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
class KnowledgeBaseInfoToolTest {

    @Mock
    private KnowledgeBaseQueryProvider provider;

    @Mock
    private AgentExecutionContext ctx;

    private KnowledgeBaseInfoTool tool;

    @BeforeEach
    void setUp() {
        tool = new KnowledgeBaseInfoTool(KnowledgeBaseInfoTool.createToolConfig(), provider);
    }

    @Test
    void createToolConfig_返回CUSTOM类型且name为default_tool_rag_info() {
        ToolConfigDTO config = KnowledgeBaseInfoTool.createToolConfig();
        assertNull(config.getId());
        assertEquals(ToolType.CUSTOM, config.getToolType());
        assertEquals("default_tool_rag_info", config.getName());
    }

    @Test
    void createToolConfig_包含描述和参数schema() {
        ToolConfigDTO config = KnowledgeBaseInfoTool.createToolConfig();
        assertNotNull(config.getDescription());
        assertFalse(config.getDescription().isBlank());
        assertNotNull(config.getParameterSchema());
        assertTrue(config.getParameterSchema().contains("\"properties\""));
    }

    @Test
    void execute_正常路径_使用ctx会话ID返回知识库信息列表() throws Exception {
        when(ctx.getSessionId()).thenReturn("s1");
        KnowledgeBaseInfo info = new KnowledgeBaseInfo("1", "kb-name", "kb-desc");
        when(provider.getKnowledgeBaseInfo("s1")).thenReturn(List.of(info));

        String result = tool.execute(ctx, "{}");

        assertTrue(result.contains("\"kbId\":\"1\""));
        assertTrue(result.contains("\"kbName\":\"kb-name\""));
        assertTrue(result.contains("\"kbDescription\":\"kb-desc\""));
        verify(provider).getKnowledgeBaseInfo("s1");
    }

    @Test
    void execute_provider返回null_序列化为空列表() throws Exception {
        when(ctx.getSessionId()).thenReturn("s1");
        when(provider.getKnowledgeBaseInfo("s1")).thenReturn(null);

        String result = tool.execute(ctx, "{}");

        assertEquals("[]", result);
        verify(provider).getKnowledgeBaseInfo("s1");
    }

    @Test
    void execute_ctx会话ID为null_返回错误() throws Exception {
        when(ctx.getSessionId()).thenReturn(null);

        String result = tool.execute(ctx, "{}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("会话"));
        verify(provider, never()).getKnowledgeBaseInfo(anyString());
    }

    @Test
    void execute_provider抛出异常_返回错误JSON() throws Exception {
        when(ctx.getSessionId()).thenReturn("s1");
        when(provider.getKnowledgeBaseInfo("s1")).thenThrow(new RuntimeException("查询失败"));

        String result = tool.execute(ctx, "{}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("查询失败"));
    }

    @Test
    void execute_provider异常消息含特殊字符_返回合法JSON() throws Exception {
        when(ctx.getSessionId()).thenReturn("s1");
        String specialMsg = "查询失败: \"引号\" \n 第二行 \\ 反斜杠";
        when(provider.getKnowledgeBaseInfo("s1")).thenThrow(new RuntimeException(specialMsg));

        String result = tool.execute(ctx, "{}");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = assertDoesNotThrow(() -> mapper.readTree(result));
        assertEquals("error", root.get("status").asText());
        assertTrue(root.get("errMsg").asText().contains("引号"));
        assertTrue(root.get("errMsg").asText().contains("第二行"));
    }
}
