package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.dto.tool.McpExpandedToolDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        when(dataProvider.loadAgentContext(sessionId)).thenReturn(
                new ContextDataProvider.AgentContextData(agentId, "test prompt", 200L, 10, List.of(), Map.of(), null, null));
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

        when(dataProvider.loadAgentContext(sessionId)).thenReturn(
                new ContextDataProvider.AgentContextData(agentId, "test prompt", 200L, 10, List.of(skill), Map.of(), null, null));
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

        when(dataProvider.loadAgentContext(sessionId)).thenReturn(
                new ContextDataProvider.AgentContextData(agentId, "test prompt", 200L, 10, List.of(skill), Map.of(), null, null));

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

        when(dataProvider.loadAgentContext(sessionId)).thenReturn(
                new ContextDataProvider.AgentContextData(agentId, "test prompt", 200L, 10, List.of(skill), Map.of(), null, null));

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

        when(dataProvider.loadAgentContext(sessionId)).thenReturn(
                new ContextDataProvider.AgentContextData(agentId, "test prompt", 200L, 10, List.of(skill), Map.of(), null, null));
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

        when(dataProvider.loadAgentContext(sessionId)).thenReturn(
                new ContextDataProvider.AgentContextData(agentId, "test prompt", 200L, 10, List.of(skill), Map.of(), null, null));

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

        when(dataProvider.loadAgentContext(sessionId)).thenReturn(
                new ContextDataProvider.AgentContextData(agentId, "test prompt", 200L, 10, List.of(skill), Map.of(), null, null));
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

        AgentContextManager.AgentSessionContext sessionContext = agentContextManager.build(sessionId).build();
        AgentExecutionContext context = sessionContext.context();

        assertTrue(context.getSkills().isEmpty());
        verify(toolManager, never()).expandMcpTools(any());
    }

    @Test
    void 正向_agentId为null时build正常返回() {
        when(dataProvider.loadAgentContext(sessionId)).thenReturn(
                new ContextDataProvider.AgentContextData(null, "test prompt", 200L, 10, List.of(), Map.of(), null, null));
        when(sessionManager.getMessages(sessionId)).thenReturn(List.of());
        when(toolManager.getSessionTools(sessionId)).thenReturn(List.of());

        AgentContextManager.AgentSessionContext sessionContext = agentContextManager.build(sessionId).build();

        assertNotNull(sessionContext);
        assertNull(sessionContext.context().getAgentId());
    }

    @Test
    void 反向_ctxData为null时抛出BusinessException() {
        when(dataProvider.loadAgentContext(sessionId)).thenReturn(null);

        assertThrows(BusinessException.class, () -> agentContextManager.build(sessionId).build());
    }

    @Nested
    class SendUserMessageTest {

        private final Long childSessionId = 2L;
        private final String testContent = "Hello from test";
        private final Long modelId = 200L;

        @BeforeEach
        void setUp() {
            when(dataProvider.loadAgentContext(sessionId)).thenReturn(
                    new ContextDataProvider.AgentContextData(agentId, "test prompt", modelId, 10, List.of(), new HashMap<>(), null, null));
            when(sessionManager.getMessages(sessionId)).thenReturn(List.of());
            when(toolManager.getSessionTools(sessionId)).thenReturn(List.of());
        }

        @Test
        void 正向_sendUserMessage返回role为user() {
            SessionManager.MessageSaveBuilder mockBuilder = mock(SessionManager.MessageSaveBuilder.class);
            when(mockBuilder.sessionId(any())).thenReturn(mockBuilder);
            when(mockBuilder.role(any())).thenReturn(mockBuilder);
            when(mockBuilder.content(any())).thenReturn(mockBuilder);
            when(mockBuilder.save()).thenReturn(1L);
            when(sessionManager.messageSave()).thenReturn(mockBuilder);

            AgentExecutionContext context = agentContextManager.build(sessionId).build().context();
            Message msg = context.sendUserMessage(childSessionId, testContent, modelId);

            assertEquals("user", msg.getRole());
        }

        @Test
        void 正向_sendUserMessage返回content与传入一致() {
            SessionManager.MessageSaveBuilder mockBuilder = mock(SessionManager.MessageSaveBuilder.class);
            when(mockBuilder.sessionId(any())).thenReturn(mockBuilder);
            when(mockBuilder.role(any())).thenReturn(mockBuilder);
            when(mockBuilder.content(any())).thenReturn(mockBuilder);
            when(mockBuilder.save()).thenReturn(1L);
            when(sessionManager.messageSave()).thenReturn(mockBuilder);

            AgentExecutionContext context = agentContextManager.build(sessionId).build().context();
            Message msg = context.sendUserMessage(childSessionId, testContent, modelId);

            assertEquals(testContent, msg.getContent());
        }

        @Test
        void 正向_sendUserMessage调用sessionManager_messageSave持久化消息() {
            SessionManager.MessageSaveBuilder mockBuilder = mock(SessionManager.MessageSaveBuilder.class);
            when(mockBuilder.sessionId(any())).thenReturn(mockBuilder);
            when(mockBuilder.role(any())).thenReturn(mockBuilder);
            when(mockBuilder.content(any())).thenReturn(mockBuilder);
            when(mockBuilder.save()).thenReturn(1L);
            when(sessionManager.messageSave()).thenReturn(mockBuilder);

            AgentExecutionContext context = agentContextManager.build(sessionId).build().context();
            context.sendUserMessage(childSessionId, testContent, modelId);

            verify(mockBuilder).sessionId(childSessionId);
            verify(mockBuilder).role("user");
            verify(mockBuilder).content(testContent);
            verify(mockBuilder).save();
        }

        @Test
        void 正向_无parentSessionId的会话sendUserMessage正常执行() {
            SessionManager.MessageSaveBuilder mockBuilder = mock(SessionManager.MessageSaveBuilder.class);
            when(mockBuilder.sessionId(any())).thenReturn(mockBuilder);
            when(mockBuilder.role(any())).thenReturn(mockBuilder);
            when(mockBuilder.content(any())).thenReturn(mockBuilder);
            when(mockBuilder.save()).thenReturn(1L);
            when(sessionManager.messageSave()).thenReturn(mockBuilder);

            AgentExecutionContext context = agentContextManager.build(sessionId).build().context();
            Message msg = context.sendUserMessage(childSessionId, testContent, modelId);

            assertNotNull(msg);
            assertEquals("user", msg.getRole());
            assertEquals(testContent, msg.getContent());
        }
    }

    @Nested
    class ParentChildSessionTest {

        private final Long parentSessionId = 1L;
        private final Long childSessionId = 2L;

        @BeforeEach
        void setUpParent() {
            when(dataProvider.loadAgentContext(parentSessionId)).thenReturn(
                    new ContextDataProvider.AgentContextData(agentId, "parent prompt", 200L, 10, List.of(), new HashMap<>(), null, null));
            when(sessionManager.getMessages(parentSessionId)).thenReturn(List.of());
            when(toolManager.getSessionTools(parentSessionId)).thenReturn(List.of());

            agentContextManager.build(parentSessionId).build();
        }

        private void stubChildSession(Long childId, Long parentId) {
            when(dataProvider.loadAgentContext(childId)).thenReturn(
                    new ContextDataProvider.AgentContextData(agentId, "child prompt", 200L, 10, List.of(), new HashMap<>(), parentId, null));
            when(sessionManager.getMessages(childId)).thenReturn(List.of());
            when(toolManager.getSessionTools(childId)).thenReturn(List.of());
        }

        @Test
        void 正向_构建时传入parentSessionId能正常构建上下文() {
            stubChildSession(childSessionId, parentSessionId);

            AgentExecutionContext childContext = agentContextManager.build(childSessionId).build().context();

            assertNotNull(childContext);
            assertEquals(parentSessionId, childContext.getParentSessionId());
        }

        @Test
        void 正向_构建时传入childSessions在context中可见() {
            Long freshSessionId = 100L;
            var childSession = new AgentExecutionContext.ChildSession(10L, "sub-agent", "test sub", 300L);
            when(dataProvider.loadAgentContext(freshSessionId)).thenReturn(
                    new ContextDataProvider.AgentContextData(agentId, "parent prompt", 200L, 10, List.of(), new HashMap<>(), null, List.of(childSession)));
            when(sessionManager.getMessages(freshSessionId)).thenReturn(List.of());
            when(toolManager.getSessionTools(freshSessionId)).thenReturn(List.of());

            AgentExecutionContext parentContext = agentContextManager.build(freshSessionId).build().context();

            assertEquals(1, parentContext.getChildSessions().size());
            assertEquals(Long.valueOf(10L), parentContext.getChildSessions().get(0).sessionId());
            assertEquals("sub-agent", parentContext.getChildSessions().get(0).sessionName());
        }

        @Test
        void 正向_子会话getParentSessionId返回父会话ID() {
            stubChildSession(childSessionId, parentSessionId);

            AgentExecutionContext childContext = agentContextManager.build(childSessionId).build().context();

            assertEquals(parentSessionId, childContext.getParentSessionId());
        }

        @Test
        void 正向_子会话putSessionVariable写入父context的sessionVariables() {
            stubChildSession(childSessionId, parentSessionId);

            AgentContextManager.AgentSessionContext childCtx = agentContextManager.build(childSessionId).build();
            AgentContextManager.AgentSessionContext parentCtx = agentContextManager.get(parentSessionId);

            childCtx.context().putSessionVariable("childKey", "childValue");

            assertEquals("childValue", parentCtx.context().getSessionVariable("childKey"));
        }

        @Test
        void 正向_子会话removeSessionVariable删除父context的sessionVariables() {
            stubChildSession(childSessionId, parentSessionId);

            AgentContextManager.AgentSessionContext childCtx = agentContextManager.build(childSessionId).build();
            AgentContextManager.AgentSessionContext parentCtx = agentContextManager.get(parentSessionId);

            parentCtx.context().putSessionVariable("keyToRemove", "toBeRemoved");
            assertTrue(parentCtx.context().getSessionVariableKeys().contains("keyToRemove"));

            childCtx.context().removeSessionVariable("keyToRemove");

            assertNull(parentCtx.context().getSessionVariable("keyToRemove"));
        }

        @Test
        void 正向_子会话build时父上下文被自动构建并缓存() {
            Long autoParentId = 100L;
            Long autoChildId = 101L;
            when(dataProvider.loadAgentContext(autoParentId)).thenReturn(
                    new ContextDataProvider.AgentContextData(agentId, "auto parent", 200L, 10, List.of(), new HashMap<>(), null, null));
            when(sessionManager.getMessages(autoParentId)).thenReturn(List.of());
            when(toolManager.getSessionTools(autoParentId)).thenReturn(List.of());

            stubChildSession(autoChildId, autoParentId);

            assertNull(agentContextManager.get(autoParentId), "构建前父上下文不在缓存中");

            AgentContextManager.AgentSessionContext childCtx = agentContextManager.build(autoChildId).build();

            assertNotNull(childCtx);
            AgentContextManager.AgentSessionContext autoParentCtx = agentContextManager.get(autoParentId);
            assertNotNull(autoParentCtx, "父上下文被自动构建并缓存");
            assertEquals(autoParentId, autoParentCtx.context().getSessionId());
        }

        @Test
        void 反向_parentSessionId对应父session不存在时子会话构建抛异常() {
            Long nonExistentParent = 999L;
            Long orphanSessionId = 3L;
            stubChildSession(orphanSessionId, nonExistentParent);

            assertThrows(BusinessException.class,
                    () -> agentContextManager.build(orphanSessionId).build());
        }

        @Test
        void 正向_子会话getSessionVariable读取父context的值() {
            stubChildSession(childSessionId, parentSessionId);

            AgentContextManager.AgentSessionContext pCtx = agentContextManager.get(parentSessionId);
            pCtx.context().putSessionVariable("parentShared", "sharedVal");

            AgentContextManager.AgentSessionContext childCtx = agentContextManager.build(childSessionId).build();

            assertEquals("sharedVal", childCtx.context().getSessionVariable("parentShared"));
        }

        @Test
        void 正向_子会话getSessionVariableKeys返回父context的keys() {
            stubChildSession(childSessionId, parentSessionId);

            AgentContextManager.AgentSessionContext pCtx = agentContextManager.get(parentSessionId);
            pCtx.context().putSessionVariable("pk1", "pv1");
            pCtx.context().putSessionVariable("pk2", "pv2");

            AgentContextManager.AgentSessionContext childCtx = agentContextManager.build(childSessionId).build();

            Set<String> keys = childCtx.context().getSessionVariableKeys();
            assertTrue(keys.contains("pk1"));
            assertTrue(keys.contains("pk2"));
        }

        @Test
        void 正向_子会话getConversationVariable委托父context() {
            stubChildSession(childSessionId, parentSessionId);

            AgentContextManager.AgentSessionContext pCtx = agentContextManager.get(parentSessionId);
            pCtx.context().putConversationVariable("convKey", "convVal");

            AgentContextManager.AgentSessionContext childCtx = agentContextManager.build(childSessionId).build();

            assertEquals("convVal", childCtx.context().getConversationVariable("convKey"));
        }

        @Test
        void 正向_子会话getConversationVariableKeys委托父context() {
            stubChildSession(childSessionId, parentSessionId);

            AgentContextManager.AgentSessionContext pCtx = agentContextManager.get(parentSessionId);
            pCtx.context().putConversationVariable("ck1", "cv1");

            AgentContextManager.AgentSessionContext childCtx = agentContextManager.build(childSessionId).build();

            assertTrue(childCtx.context().getConversationVariableKeys().contains("ck1"));
        }

        @Test
        void 反向_子会话putSessionVariable不影响本地sessionVariables() throws Exception {
            stubChildSession(childSessionId, parentSessionId);

            AgentContextManager.AgentSessionContext childCtx = agentContextManager.build(childSessionId).build();

            childCtx.context().putSessionVariable("childPut", "childVal");

            java.lang.reflect.Field localVars = AgentExecutionContext.class.getDeclaredField("sessionVariables");
            localVars.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> childLocal = (Map<String, String>) localVars.get(childCtx.context());
            assertFalse(childLocal.containsKey("childPut"), "子会话的本地sessionVariables不应包含通过put写入的key");
        }

        @Test
        void 反向_子会话putConversationVariable不影响本地conversationVariables() throws Exception {
            stubChildSession(childSessionId, parentSessionId);

            AgentContextManager.AgentSessionContext childCtx = agentContextManager.build(childSessionId).build();

            childCtx.context().putConversationVariable("childConv", "convVal");

            java.lang.reflect.Field localVars = AgentExecutionContext.class.getDeclaredField("conversationVariables");
            localVars.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> childLocal = (Map<String, String>) localVars.get(childCtx.context());
            assertFalse(childLocal.containsKey("childConv"), "子会话的本地conversationVariables不应包含通过put写入的key");
        }

        @Test
        void 反向_子会话removeSessionVariable不影响本地sessionVariables() throws Exception {
            stubChildSession(childSessionId, parentSessionId);

            AgentContextManager.AgentSessionContext childCtx = agentContextManager.build(childSessionId).build();

            AgentContextManager.AgentSessionContext pCtx = agentContextManager.get(parentSessionId);
            pCtx.context().putSessionVariable("toBeRemoved", "val");

            childCtx.context().removeSessionVariable("toBeRemoved");

            java.lang.reflect.Field localVars = AgentExecutionContext.class.getDeclaredField("sessionVariables");
            localVars.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> childLocal = (Map<String, String>) localVars.get(childCtx.context());
            assertFalse(childLocal.containsKey("toBeRemoved"), "子会话的本地sessionVariables不应包含已删除的key");
        }

        @Test
        void 反向_子会话removeConversationVariable不影响本地conversationVariables() throws Exception {
            stubChildSession(childSessionId, parentSessionId);

            AgentContextManager.AgentSessionContext childCtx = agentContextManager.build(childSessionId).build();

            AgentContextManager.AgentSessionContext pCtx = agentContextManager.get(parentSessionId);
            pCtx.context().putConversationVariable("toBeRemoved", "val");

            childCtx.context().removeConversationVariable("toBeRemoved");

            java.lang.reflect.Field localVars = AgentExecutionContext.class.getDeclaredField("conversationVariables");
            localVars.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> childLocal = (Map<String, String>) localVars.get(childCtx.context());
            assertFalse(childLocal.containsKey("toBeRemoved"), "子会话的本地conversationVariables不应包含已删除的key");
        }
    }
}
