package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.dto.tool.McpExpandedToolDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentContextManagerTest {

    @Mock
    private ContextDataProvider dataProvider;
    @Mock
    private SessionManager sessionManager;
    @Mock
    private ToolManager toolManager;

    private AgentContextManager agentContextManager;

    private final Long sessionId = 1L;
    private final Long agentId = 100L;

    @BeforeEach
    void setUp() {
        agentContextManager = new AgentContextManager(dataProvider, sessionManager, toolManager);
    }

    private void stubBasicContext() {
        when(dataProvider.getAgentId(sessionId)).thenReturn(agentId);
        when(dataProvider.getSystemPrompt(agentId)).thenReturn("test prompt");
        when(dataProvider.getDefaultModelId(agentId)).thenReturn(200L);
        when(dataProvider.getRecentMessageCount(agentId)).thenReturn(10);
        when(dataProvider.loadSessionVariables(sessionId)).thenReturn(Map.of());
        when(sessionManager.getMessages(sessionId)).thenReturn(List.of());
        when(toolManager.getSessionTools(sessionId)).thenReturn(List.of());
    }

    @Test
    void 正向_MCP工具被展开_非MCP工具保持不变() {
        stubBasicContext();

        ToolConfigDTO mcpTool = ToolConfigDTO.builder()
                .name("mcp_tool")
                .toolType(ToolType.MCP_HTTP)
                .implPath("http://localhost/mcp")
                .authConfig("{}")
                .build();

        ToolConfigDTO javaTool = ToolConfigDTO.builder()
                .name("java_tool")
                .toolType(ToolType.JAVA)
                .implPath("com.example.MyTool")
                .build();

        McpExpandedToolDTO expanded1 = McpExpandedToolDTO.builder()
                .name("mcp_tool_func1")
                .toolType(ToolType.MCP_HTTP)
                .remoteToolName("func1")
                .build();
        McpExpandedToolDTO expanded2 = McpExpandedToolDTO.builder()
                .name("mcp_tool_func2")
                .toolType(ToolType.MCP_HTTP)
                .remoteToolName("func2")
                .build();

        SkillConfigDTO skill = SkillConfigDTO.builder()
                .name("test_skill")
                .skillTools(List.of(mcpTool, javaTool))
                .build();

        when(dataProvider.loadSkills(agentId)).thenReturn(List.of(skill));
        when(toolManager.expandMcpTools(mcpTool)).thenReturn(List.of(expanded1, expanded2));

        AgentContextManager.AgentSessionContext sessionContext = agentContextManager.build(sessionId).build();
        AgentExecutionContext context = sessionContext.context();
        List<SkillConfigDTO> resultSkills = context.getSkills();

        assertEquals(1, resultSkills.size());
        SkillConfigDTO resultSkill = resultSkills.get(0);
        List<ToolConfigDTO> resultTools = resultSkill.getSkillTools();

        assertEquals(3, resultTools.size());
        assertEquals("mcp_tool_func1", resultTools.get(0).getName());
        assertEquals(ToolType.MCP_HTTP, resultTools.get(0).getToolType());
        assertInstanceOf(McpExpandedToolDTO.class, resultTools.get(0));

        assertEquals("mcp_tool_func2", resultTools.get(1).getName());
        assertEquals(ToolType.MCP_HTTP, resultTools.get(1).getToolType());
        assertInstanceOf(McpExpandedToolDTO.class, resultTools.get(1));

        assertSame(javaTool, resultTools.get(2));
        assertEquals(ToolType.JAVA, resultTools.get(2).getToolType());
    }

    @Test
    void 反向_skillTools为null时不抛出异常() {
        stubBasicContext();

        SkillConfigDTO skill = SkillConfigDTO.builder()
                .name("null_tools_skill")
                .skillTools(null)
                .build();

        when(dataProvider.loadSkills(agentId)).thenReturn(List.of(skill));

        AgentContextManager.AgentSessionContext sessionContext = agentContextManager.build(sessionId).build();
        AgentExecutionContext context = sessionContext.context();
        List<SkillConfigDTO> resultSkills = context.getSkills();

        assertEquals(1, resultSkills.size());
        assertNull(resultSkills.get(0).getSkillTools());
    }

    @Test
    void 反向_skillTools为空列表时不处理() {
        stubBasicContext();

        SkillConfigDTO skill = SkillConfigDTO.builder()
                .name("empty_tools_skill")
                .skillTools(List.of())
                .build();

        when(dataProvider.loadSkills(agentId)).thenReturn(List.of(skill));

        AgentContextManager.AgentSessionContext sessionContext = agentContextManager.build(sessionId).build();
        AgentExecutionContext context = sessionContext.context();
        List<SkillConfigDTO> resultSkills = context.getSkills();

        assertEquals(1, resultSkills.size());
        assertTrue(resultSkills.get(0).getSkillTools().isEmpty());
    }

    @Test
    void 边界_技能中全部为MCP工具时全部展开() {
        stubBasicContext();

        ToolConfigDTO mcp1 = ToolConfigDTO.builder().name("mcp1").toolType(ToolType.MCP_HTTP).implPath("http://a").authConfig("{}").build();
        ToolConfigDTO mcp2 = ToolConfigDTO.builder().name("mcp2").toolType(ToolType.MCP_HTTP).implPath("http://b").authConfig("{}").build();

        McpExpandedToolDTO e1 = McpExpandedToolDTO.builder().name("mcp1_f1").toolType(ToolType.MCP_HTTP).remoteToolName("f1").build();
        McpExpandedToolDTO e2 = McpExpandedToolDTO.builder().name("mcp2_f1").toolType(ToolType.MCP_HTTP).remoteToolName("f1").build();

        SkillConfigDTO skill = SkillConfigDTO.builder().name("all_mcp").skillTools(List.of(mcp1, mcp2)).build();

        when(dataProvider.loadSkills(agentId)).thenReturn(List.of(skill));
        when(toolManager.expandMcpTools(mcp1)).thenReturn(List.of(e1));
        when(toolManager.expandMcpTools(mcp2)).thenReturn(List.of(e2));

        AgentContextManager.AgentSessionContext sessionContext = agentContextManager.build(sessionId).build();
        AgentExecutionContext context = sessionContext.context();
        List<ToolConfigDTO> resultTools = context.getSkills().get(0).getSkillTools();

        assertEquals(2, resultTools.size());
        assertEquals("mcp1_f1", resultTools.get(0).getName());
        assertEquals("mcp2_f1", resultTools.get(1).getName());
    }

    @Test
    void 边界_技能中无MCP工具时所有工具保持不变() {
        stubBasicContext();

        ToolConfigDTO javaTool = ToolConfigDTO.builder().name("jt").toolType(ToolType.JAVA).build();
        ToolConfigDTO tsTool = ToolConfigDTO.builder().name("ts").toolType(ToolType.TYPESCRIPT).build();

        SkillConfigDTO skill = SkillConfigDTO.builder().name("no_mcp").skillTools(List.of(javaTool, tsTool)).build();

        when(dataProvider.loadSkills(agentId)).thenReturn(List.of(skill));

        AgentContextManager.AgentSessionContext sessionContext = agentContextManager.build(sessionId).build();
        AgentExecutionContext context = sessionContext.context();
        List<ToolConfigDTO> resultTools = context.getSkills().get(0).getSkillTools();

        assertEquals(2, resultTools.size());
        assertSame(javaTool, resultTools.get(0));
        assertSame(tsTool, resultTools.get(1));
    }

    @Test
    void 反向_已展开的McpExpandedToolDTO不会被二次展开() {
        stubBasicContext();

        ToolConfigDTO mcpTool = ToolConfigDTO.builder()
                .name("mcp_tool")
                .toolType(ToolType.MCP_HTTP)
                .implPath("http://localhost/mcp")
                .authConfig("{}")
                .build();

        McpExpandedToolDTO alreadyExpanded = McpExpandedToolDTO.builder()
                .name("mcp_tool_func1")
                .toolType(ToolType.MCP_HTTP)
                .remoteToolName("func1")
                .build();

        SkillConfigDTO skill = SkillConfigDTO.builder()
                .name("mixed_skill")
                .skillTools(List.of(mcpTool, alreadyExpanded))
                .build();

        when(dataProvider.loadSkills(agentId)).thenReturn(List.of(skill));
        when(toolManager.expandMcpTools(mcpTool)).thenReturn(List.of(
                McpExpandedToolDTO.builder().name("mcp_tool_funcA").toolType(ToolType.MCP_HTTP).remoteToolName("funcA").build()
        ));

        AgentContextManager.AgentSessionContext sessionContext = agentContextManager.build(sessionId).build();
        AgentExecutionContext context = sessionContext.context();
        List<ToolConfigDTO> resultTools = context.getSkills().get(0).getSkillTools();

        assertEquals(2, resultTools.size(), "展开后应该是 2 个工具: 新展开的1个 + 已有的1个");
        assertEquals("mcp_tool_funcA", resultTools.get(0).getName());
        assertSame(alreadyExpanded, resultTools.get(1), "已展开的工具应原样保留，不被二次展开");
        verify(toolManager, times(1)).expandMcpTools(any());
    }

    @Test
    void 边界_skills为空列表时不处理任何技能() {
        stubBasicContext();
        when(dataProvider.loadSkills(agentId)).thenReturn(List.of());

        AgentContextManager.AgentSessionContext sessionContext = agentContextManager.build(sessionId).build();
        AgentExecutionContext context = sessionContext.context();

        assertTrue(context.getSkills().isEmpty());
        verify(toolManager, never()).expandMcpTools(any());
    }
}
