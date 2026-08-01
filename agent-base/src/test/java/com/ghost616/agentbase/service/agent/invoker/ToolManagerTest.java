package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.dto.tool.McpExpandedToolDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.SessionAuthType;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
import com.ghost616.agentbase.service.agent.ToolDataProvider.SessionToolInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.*;

class ToolManagerTest {

    private ToolManager toolManager;
    private ToolDataProvider dataProvider;
    private AgentComponentRegistry registry;
    private AgentContextManager agentContextManager;
    private ConcurrentHashMap<String, ToolManager.ToolSessionObject> toolCache;

    @BeforeEach
    void setUp() throws Exception {
        registry = mock(AgentComponentRegistry.class);
        dataProvider = mock(ToolDataProvider.class);
        when(registry.getToolDataProvider()).thenReturn(dataProvider);
        agentContextManager = mock(AgentContextManager.class);
        when(registry.getAgentContextManager()).thenReturn(agentContextManager);

        toolManager = new ToolManager(registry);

        Field toolCacheField = ToolManager.class.getDeclaredField("toolCache");
        toolCacheField.setAccessible(true);
        toolCache = (ConcurrentHashMap<String, ToolManager.ToolSessionObject>) toolCacheField.get(toolManager);
    }

    @Test
    void expandMcpTools方法应该为public() throws Exception {
        Method method = ToolManager.class.getDeclaredMethod("expandMcpTools", ToolConfigDTO.class);
        assertTrue(Modifier.isPublic(method.getModifiers()));
    }

    @Test
    void expandMcpTools返回类型应为List_McpExpandedToolDTO() throws Exception {
        Method method = ToolManager.class.getDeclaredMethod("expandMcpTools", ToolConfigDTO.class);
        assertEquals(List.class, method.getReturnType());
    }

    @Test
    void getBuiltinTools应委托dataProvider并传入modelId() {
        List<Map<String, Object>> expected = List.of(Map.of("type", "web_search", "name", "web_search"));
        when(dataProvider.getBuiltinTools("m1")).thenReturn(expected);

        List<Map<String, Object>> result = toolManager.getBuiltinTools("m1");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("web_search", result.get(0).get("name"));
        verify(dataProvider).getBuiltinTools("m1");
    }

    @Test
    void 父会话非MCP工具sessionAuth使用SessionToolInfo的值() {
        String sessionId = "100";
        ToolConfigDTO dto = ToolConfigDTO.builder()
                .id("1").name("java-tool").toolType(ToolType.JAVA).implPath("com.test.Foo")
                .sessionAuth(SessionAuthType.ALL).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put("1", new ToolManager.ToolSessionObject(dto, invoker, null, List.of(), List.of()));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo("1", SessionAuthType.CHILD)));

        List<ToolManager.ToolSessionObject> result = toolManager.getSessionTools(sessionId, false);

        assertEquals(1, result.size(), "getSessionTools 返回完整工具列表，不再过滤 CHILD");
        assertEquals(SessionAuthType.CHILD, result.get(0).toolConfig().getSessionAuth());
    }

    @Test
    void 父会话MCP工具sessionAuth设为PARENT() {
        String sessionId = "200";
        ToolConfigDTO originalConfig = ToolConfigDTO.builder()
                .id("2").name("mcp-cfg").toolType(ToolType.MCP_HTTP).implPath("http://localhost/mcp")
                .sessionAuth(SessionAuthType.CHILD).build();
        McpExpandedToolDTO expanded = McpExpandedToolDTO.builder()
                .id("2").name("mcp-cfg_tool1").toolType(ToolType.MCP_HTTP)
                .implPath("http://localhost/mcp").remoteToolName("tool1")
                .sessionAuth(SessionAuthType.CHILD).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put("2", new ToolManager.ToolSessionObject(
                originalConfig, null, originalConfig, List.of(expanded), List.of(invoker)));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo("2", SessionAuthType.PARENT)));

        List<ToolManager.ToolSessionObject> result = toolManager.getSessionTools(sessionId, false);

        assertEquals(1, result.size());
        assertEquals(SessionAuthType.PARENT, result.get(0).toolConfig().getSessionAuth());
        assertEquals(SessionAuthType.CHILD, originalConfig.getSessionAuth());
    }

    @Test
    void 父会话MCP工具ALL时产生PARENT展开副本和CHILD原始配置() {
        String sessionId = "300";
        ToolConfigDTO originalConfig = ToolConfigDTO.builder()
                .id("3").name("mcp-all").toolType(ToolType.MCP_HTTP).implPath("http://localhost/mcp")
                .sessionAuth(SessionAuthType.ALL).build();
        McpExpandedToolDTO expanded = McpExpandedToolDTO.builder()
                .id("3").name("mcp-all_tool1").toolType(ToolType.MCP_HTTP)
                .implPath("http://localhost/mcp").remoteToolName("tool1")
                .sessionAuth(SessionAuthType.ALL).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put("3", new ToolManager.ToolSessionObject(
                originalConfig, null, originalConfig, List.of(expanded), List.of(invoker)));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo("3", SessionAuthType.ALL)));

        List<ToolManager.ToolSessionObject> result = toolManager.getSessionTools(sessionId, false);

        assertEquals(2, result.size(), "getSessionTools 返回完整工具列表，含 PARENT 展开和 CHILD 原始配置");
        // result[0]: PARENT copy of expanded McpExpandedToolDTO
        assertEquals(SessionAuthType.PARENT, result.get(0).toolConfig().getSessionAuth());
        assertTrue(result.get(0).toolConfig() instanceof McpExpandedToolDTO);
        assertEquals("mcp-all_tool1", result.get(0).toolConfig().getName());
        assertNotNull(result.get(0).invoker());
        // result[1]: CHILD copy of original config
        assertEquals(SessionAuthType.CHILD, result.get(1).toolConfig().getSessionAuth());
        assertFalse(result.get(1).toolConfig() instanceof McpExpandedToolDTO);
        assertEquals("mcp-all", result.get(1).toolConfig().getName());
        assertNull(result.get(1).invoker());
    }

    @Test
    void 子会话所有工具sessionAuth为PARENT() {
        String sessionId = "400";
        ToolConfigDTO dto = ToolConfigDTO.builder()
                .id("4").name("py-tool").toolType(ToolType.PYTHON).implPath("test.py")
                .sessionAuth(SessionAuthType.CHILD).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put("4", new ToolManager.ToolSessionObject(dto, invoker, null, List.of(), List.of()));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo("4", SessionAuthType.CHILD)));

        List<ToolManager.ToolSessionObject> result = toolManager.getSessionTools(sessionId, true);

        assertEquals(1, result.size());
        assertEquals(SessionAuthType.PARENT, result.get(0).toolConfig().getSessionAuth());
        assertEquals(SessionAuthType.CHILD, dto.getSessionAuth());
    }

    @Test
    void 多个工具各自按规则设置sessionAuth() {
        String sessionId = "500";
        ToolConfigDTO javaDto = ToolConfigDTO.builder()
                .id("5").name("java-tool").toolType(ToolType.JAVA).implPath("com.test.Bar")
                .sessionAuth(SessionAuthType.ALL).build();
        ToolConfigDTO mcpConfig = ToolConfigDTO.builder()
                .id("6").name("mcp-cfg").toolType(ToolType.MCP_HTTP).implPath("http://localhost/mcp")
                .sessionAuth(SessionAuthType.ALL).build();
        McpExpandedToolDTO mcpExpanded = McpExpandedToolDTO.builder()
                .id("6").name("mcp-cfg_toolA").toolType(ToolType.MCP_HTTP)
                .implPath("http://localhost/mcp").remoteToolName("toolA")
                .sessionAuth(SessionAuthType.ALL).build();
        ToolInvoker invoker1 = mock(ToolInvoker.class);
        ToolInvoker invoker2 = mock(ToolInvoker.class);
        toolCache.put("5", new ToolManager.ToolSessionObject(javaDto, invoker1, null, List.of(), List.of()));
        toolCache.put("6", new ToolManager.ToolSessionObject(
                mcpConfig, null, mcpConfig, List.of(mcpExpanded), List.of(invoker2)));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(
                        new SessionToolInfo("5", SessionAuthType.PARENT),
                        new SessionToolInfo("6", SessionAuthType.ALL)));

        List<ToolManager.ToolSessionObject> result = toolManager.getSessionTools(sessionId, false);

        // java-tool: PARENT from SessionToolInfo
        // mcp-cfg_toolA: PARENT (isMcpTool)
        // mcp-cfg: CHILD copy, now preserved
        assertEquals(3, result.size(), "getSessionTools 返回完整工具列表，含 CHILD 副本");
        // result[0]: java-tool, non-MCP, uses info.sessionAuth=PARENT
        assertEquals(SessionAuthType.PARENT, result.get(0).toolConfig().getSessionAuth());
        assertFalse(result.get(0).toolConfig() instanceof McpExpandedToolDTO);
        assertEquals("java-tool", result.get(0).toolConfig().getName());
        // result[1]: mcp-cfg_toolA, expanded MCP, PARENT copy
        assertEquals(SessionAuthType.PARENT, result.get(1).toolConfig().getSessionAuth());
        assertTrue(result.get(1).toolConfig() instanceof McpExpandedToolDTO);
        assertEquals("mcp-cfg_toolA", result.get(1).toolConfig().getName());
        // result[2]: mcp-cfg, CHILD copy
        assertEquals(SessionAuthType.CHILD, result.get(2).toolConfig().getSessionAuth());
        assertFalse(result.get(2).toolConfig() instanceof McpExpandedToolDTO);
        assertEquals("mcp-cfg", result.get(2).toolConfig().getName());
        assertNull(result.get(2).invoker());
    }

    @Test
    void CUSTOM类型provider正常时返回对应invoker() {
        String sessionId = "600";
        String toolId = "60";
        ToolConfigDTO dto = ToolConfigDTO.builder()
                .id(toolId).name("custom-tool").toolType(ToolType.CUSTOM).implPath("my.CustomImpl")
                .sessionAuth(SessionAuthType.ALL).build();

        CustomToolInvoker customInvoker = new CustomToolInvoker(dto) {
            @Override
            public String execute(com.ghost616.agentbase.service.agent.AgentExecutionContext ctx, String arguments) {
                return "custom-result";
            }
        };
        when(dataProvider.getCustomInvoker(dto)).thenReturn(customInvoker);
        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo(toolId, SessionAuthType.ALL)));
        when(dataProvider.getToolById(toolId)).thenReturn(dto);

        List<ToolManager.ToolSessionObject> result = toolManager.getSessionTools(sessionId, false);

        assertEquals(1, result.size());
        assertNotNull(result.get(0).invoker());
        assertEquals(ToolType.CUSTOM, result.get(0).toolConfig().getToolType());
    }

    @Test
    void CUSTOM类型provider为null时抛出UnsupportedOperationException() {
        String sessionId = "700";
        String toolId = "70";
        ToolConfigDTO dto = ToolConfigDTO.builder()
                .id(toolId).name("custom-null").toolType(ToolType.CUSTOM).implPath("no.Provider")
                .sessionAuth(SessionAuthType.ALL).build();

        when(dataProvider.getCustomInvoker(dto)).thenReturn(null);
        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo(toolId, SessionAuthType.ALL)));
        when(dataProvider.getToolById(toolId)).thenReturn(dto);

        assertThrows(UnsupportedOperationException.class,
                () -> toolManager.getSessionTools(sessionId, false));
    }

    @Test
    void 匹配到非MCP工具时返回toolConfig() {
        String sessionId = "800";
        ToolConfigDTO dto = ToolConfigDTO.builder()
                .id("80").name("java-tool").toolType(ToolType.JAVA).implPath("com.test.Foo")
                .sessionAuth(SessionAuthType.ALL).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put("80", new ToolManager.ToolSessionObject(dto, invoker, null, List.of(), List.of()));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo("80", SessionAuthType.ALL)));

        ToolConfigDTO result = toolManager.getToolConfig(sessionId, "java-tool");

        assertNotNull(result);
        assertEquals("java-tool", result.getName());
        assertEquals("80", result.getId());
        assertNull(toolManager.getToolConfig(sessionId, "non-existent"));
    }

    @Test
    void 匹配到MCP展开工具时返回mcpOriginalConfig() {
        String sessionId = "900";
        ToolConfigDTO originalConfig = ToolConfigDTO.builder()
                .id("90").name("mcp-cfg").toolType(ToolType.MCP_HTTP).implPath("http://localhost/mcp")
                .sessionAuth(SessionAuthType.ALL).build();
        ToolConfigDTO toolConfig = ToolConfigDTO.builder()
                .id("90").name("mcp-cfg_tool1").toolType(ToolType.MCP_HTTP)
                .implPath("http://localhost/mcp")
                .sessionAuth(SessionAuthType.PARENT).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put("90", new ToolManager.ToolSessionObject(toolConfig, invoker, originalConfig, List.of(), List.of()));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo("90", SessionAuthType.ALL)));

        ToolConfigDTO result = toolManager.getToolConfig(sessionId, "mcp-cfg_tool1");

        assertNotNull(result);
        assertSame(originalConfig, result);
    }

    @Test
    void 未匹配到toolName时返回null() {
        String sessionId = "1000";
        ToolConfigDTO dto = ToolConfigDTO.builder()
                .id("100").name("some-tool").toolType(ToolType.JAVA).implPath("com.test.Bar")
                .sessionAuth(SessionAuthType.ALL).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put("100", new ToolManager.ToolSessionObject(dto, invoker, null, List.of(), List.of()));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo("100", SessionAuthType.ALL)));

        ToolConfigDTO result = toolManager.getToolConfig(sessionId, "non-existent-tool");

        assertNull(result);
    }

    @Test
    void 遍历多个工具时正确匹配到指定名称的工具() {
        String sessionId = "1100";
        ToolConfigDTO dto1 = ToolConfigDTO.builder()
                .id("101").name("tool-alpha").toolType(ToolType.JAVA).implPath("com.test.Alpha")
                .sessionAuth(SessionAuthType.ALL).build();
        ToolConfigDTO dto2 = ToolConfigDTO.builder()
                .id("102").name("tool-beta").toolType(ToolType.PYTHON).implPath("beta.py")
                .sessionAuth(SessionAuthType.CHILD).build();
        ToolConfigDTO dto3 = ToolConfigDTO.builder()
                .id("103").name("tool-gamma").toolType(ToolType.TYPESCRIPT).implPath("gamma.ts")
                .sessionAuth(SessionAuthType.PARENT).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put("101", new ToolManager.ToolSessionObject(dto1, invoker, null, List.of(), List.of()));
        toolCache.put("102", new ToolManager.ToolSessionObject(dto2, invoker, null, List.of(), List.of()));
        toolCache.put("103", new ToolManager.ToolSessionObject(dto3, invoker, null, List.of(), List.of()));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(
                        new SessionToolInfo("101", SessionAuthType.ALL),
                        new SessionToolInfo("102", SessionAuthType.ALL),
                        new SessionToolInfo("103", SessionAuthType.ALL)));

        ToolConfigDTO result = toolManager.getToolConfig(sessionId, "tool-beta");

        assertNotNull(result);
        assertEquals("tool-beta", result.getName());
        assertEquals("102", result.getId());
        assertEquals(ToolType.PYTHON, result.getToolType());
    }

    @Test
    void 主会话获取CHILD工具invoker应返回null() {
        String sessionId = "getInvoker-main-session";
        ToolConfigDTO dto = ToolConfigDTO.builder()
                .id("child-inv-1").name("child-tool").toolType(ToolType.JAVA).implPath("com.test.Child")
                .sessionAuth(SessionAuthType.CHILD).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put("child-inv-1", new ToolManager.ToolSessionObject(dto, invoker, null, List.of(), List.of()));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo("child-inv-1", SessionAuthType.CHILD)));

        ToolInvoker result = toolManager.getInvoker(sessionId, "child-tool");

        assertNull(result);
    }

    @Test
    void 子会话获取CHILD工具invoker应返回invoker() {
        String sessionId = "getInvoker-sub-session";
        ToolConfigDTO dto = ToolConfigDTO.builder()
                .id("child-inv-2").name("child-tool-2").toolType(ToolType.JAVA).implPath("com.test.Child2")
                .sessionAuth(SessionAuthType.CHILD).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put("child-inv-2", new ToolManager.ToolSessionObject(dto, invoker, null, List.of(), List.of()));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo("child-inv-2", SessionAuthType.CHILD)));

        AgentContextManager.AgentSessionContext mockCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext mockExecCtx = mock(AgentExecutionContext.class);
        when(mockCtx.context()).thenReturn(mockExecCtx);
        when(mockExecCtx.isMainSession()).thenReturn(false);
        when(agentContextManager.get(sessionId)).thenReturn(mockCtx);

        ToolInvoker result = toolManager.getInvoker(sessionId, "child-tool-2");

        assertNotNull(result);
        assertSame(invoker, result);
    }

    @Test
    void 主会话获取非CHILD工具invoker行为不变() {
        String sessionId = "getInvoker-main-session-2";
        ToolConfigDTO dto = ToolConfigDTO.builder()
                .id("normal-inv-1").name("normal-tool").toolType(ToolType.JAVA).implPath("com.test.Normal")
                .sessionAuth(SessionAuthType.ALL).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put("normal-inv-1", new ToolManager.ToolSessionObject(dto, invoker, null, List.of(), List.of()));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo("normal-inv-1", SessionAuthType.PARENT)));

        ToolInvoker result = toolManager.getInvoker(sessionId, "normal-tool");

        assertNotNull(result);
        assertSame(invoker, result);
    }
}
