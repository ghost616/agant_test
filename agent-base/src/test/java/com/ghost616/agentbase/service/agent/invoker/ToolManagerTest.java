package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.dto.tool.McpExpandedToolDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.SessionAuthType;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
import com.ghost616.agentbase.service.agent.ToolDataProvider.SessionToolInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ToolManagerTest {

    private ToolManager toolManager;
    private ToolDataProvider dataProvider;
    private AgentComponentRegistry registry;
    private ConcurrentHashMap<Long, ToolManager.ToolSessionObject> toolCache;

    @BeforeEach
    void setUp() throws Exception {
        registry = mock(AgentComponentRegistry.class);
        dataProvider = mock(ToolDataProvider.class);
        when(registry.getToolDataProvider()).thenReturn(dataProvider);
        when(registry.getAgentContextManager()).thenReturn(mock(AgentContextManager.class));

        toolManager = new ToolManager(registry);

        Field toolCacheField = ToolManager.class.getDeclaredField("toolCache");
        toolCacheField.setAccessible(true);
        toolCache = (ConcurrentHashMap<Long, ToolManager.ToolSessionObject>) toolCacheField.get(toolManager);
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
    void 父会话非MCP工具sessionAuth使用SessionToolInfo的值() {
        Long sessionId = 100L;
        ToolConfigDTO dto = ToolConfigDTO.builder()
                .id(1L).name("java-tool").toolType(ToolType.JAVA).implPath("com.test.Foo")
                .sessionAuth(SessionAuthType.ALL).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put(1L, new ToolManager.ToolSessionObject(dto, invoker, null, List.of(), List.of()));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo(1L, SessionAuthType.CHILD)));

        List<ToolManager.ToolSessionObject> result = toolManager.getSessionTools(sessionId, false);

        assertEquals(1, result.size());
        assertEquals(SessionAuthType.CHILD, result.get(0).toolConfig().getSessionAuth());
        assertEquals(SessionAuthType.ALL, dto.getSessionAuth());
    }

    @Test
    void 父会话MCP工具sessionAuth设为PARENT() {
        Long sessionId = 200L;
        ToolConfigDTO originalConfig = ToolConfigDTO.builder()
                .id(2L).name("mcp-cfg").toolType(ToolType.MCP_HTTP).implPath("http://localhost/mcp")
                .sessionAuth(SessionAuthType.CHILD).build();
        McpExpandedToolDTO expanded = McpExpandedToolDTO.builder()
                .id(2L).name("mcp-cfg_tool1").toolType(ToolType.MCP_HTTP)
                .implPath("http://localhost/mcp").remoteToolName("tool1")
                .sessionAuth(SessionAuthType.CHILD).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put(2L, new ToolManager.ToolSessionObject(
                originalConfig, null, originalConfig, List.of(expanded), List.of(invoker)));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo(2L, SessionAuthType.PARENT)));

        List<ToolManager.ToolSessionObject> result = toolManager.getSessionTools(sessionId, false);

        assertEquals(1, result.size());
        assertEquals(SessionAuthType.PARENT, result.get(0).toolConfig().getSessionAuth());
        assertEquals(SessionAuthType.CHILD, originalConfig.getSessionAuth());
    }

    @Test
    void 父会话MCP工具ALL时产生PARENT展开副本和一份CHILD原始配置() {
        Long sessionId = 300L;
        ToolConfigDTO originalConfig = ToolConfigDTO.builder()
                .id(3L).name("mcp-all").toolType(ToolType.MCP_HTTP).implPath("http://localhost/mcp")
                .sessionAuth(SessionAuthType.ALL).build();
        McpExpandedToolDTO expanded = McpExpandedToolDTO.builder()
                .id(3L).name("mcp-all_tool1").toolType(ToolType.MCP_HTTP)
                .implPath("http://localhost/mcp").remoteToolName("tool1")
                .sessionAuth(SessionAuthType.ALL).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put(3L, new ToolManager.ToolSessionObject(
                originalConfig, null, originalConfig, List.of(expanded), List.of(invoker)));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo(3L, SessionAuthType.ALL)));

        List<ToolManager.ToolSessionObject> result = toolManager.getSessionTools(sessionId, false);

        assertEquals(2, result.size());
        // result[0]: PARENT copy of expanded McpExpandedToolDTO
        assertEquals(SessionAuthType.PARENT, result.get(0).toolConfig().getSessionAuth());
        assertTrue(result.get(0).toolConfig() instanceof McpExpandedToolDTO);
        assertEquals("mcp-all_tool1", result.get(0).toolConfig().getName());
        assertNotNull(result.get(0).invoker());
        // result[1]: single CHILD copy of original raw config
        assertEquals(SessionAuthType.CHILD, result.get(1).toolConfig().getSessionAuth());
        assertFalse(result.get(1).toolConfig() instanceof McpExpandedToolDTO);
        assertEquals("mcp-all", result.get(1).toolConfig().getName());
        assertNull(result.get(1).invoker());
        assertSame(result.get(1).toolConfig(), result.get(1).mcpOriginalConfig());
    }

    @Test
    void 子会话所有工具sessionAuth为PARENT() {
        Long sessionId = 400L;
        ToolConfigDTO dto = ToolConfigDTO.builder()
                .id(4L).name("py-tool").toolType(ToolType.PYTHON).implPath("test.py")
                .sessionAuth(SessionAuthType.CHILD).build();
        ToolInvoker invoker = mock(ToolInvoker.class);
        toolCache.put(4L, new ToolManager.ToolSessionObject(dto, invoker, null, List.of(), List.of()));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo(4L, SessionAuthType.CHILD)));

        List<ToolManager.ToolSessionObject> result = toolManager.getSessionTools(sessionId, true);

        assertEquals(1, result.size());
        assertEquals(SessionAuthType.PARENT, result.get(0).toolConfig().getSessionAuth());
        assertEquals(SessionAuthType.CHILD, dto.getSessionAuth());
    }

    @Test
    void 多个工具各自按规则设置sessionAuth() {
        Long sessionId = 500L;
        ToolConfigDTO javaDto = ToolConfigDTO.builder()
                .id(5L).name("java-tool").toolType(ToolType.JAVA).implPath("com.test.Bar")
                .sessionAuth(SessionAuthType.ALL).build();
        ToolConfigDTO mcpConfig = ToolConfigDTO.builder()
                .id(6L).name("mcp-cfg").toolType(ToolType.MCP_HTTP).implPath("http://localhost/mcp")
                .sessionAuth(SessionAuthType.ALL).build();
        McpExpandedToolDTO mcpExpanded = McpExpandedToolDTO.builder()
                .id(6L).name("mcp-cfg_toolA").toolType(ToolType.MCP_HTTP)
                .implPath("http://localhost/mcp").remoteToolName("toolA")
                .sessionAuth(SessionAuthType.ALL).build();
        ToolInvoker invoker1 = mock(ToolInvoker.class);
        ToolInvoker invoker2 = mock(ToolInvoker.class);
        toolCache.put(5L, new ToolManager.ToolSessionObject(javaDto, invoker1, null, List.of(), List.of()));
        toolCache.put(6L, new ToolManager.ToolSessionObject(
                mcpConfig, null, mcpConfig, List.of(mcpExpanded), List.of(invoker2)));

        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(
                        new SessionToolInfo(5L, SessionAuthType.PARENT),
                        new SessionToolInfo(6L, SessionAuthType.ALL)));

        List<ToolManager.ToolSessionObject> result = toolManager.getSessionTools(sessionId, false);

        // java-tool: PARENT from SessionToolInfo
        // mcp-cfg_toolA: PARENT (isMcpTool)
        // mcp-cfg: CHILD (one raw copy for ALL)
        assertEquals(3, result.size());
        // result[0]: java-tool, non-MCP, uses info.sessionAuth=PARENT
        assertEquals(SessionAuthType.PARENT, result.get(0).toolConfig().getSessionAuth());
        assertFalse(result.get(0).toolConfig() instanceof McpExpandedToolDTO);
        assertEquals("java-tool", result.get(0).toolConfig().getName());
        // result[1]: mcp-cfg_toolA, expanded MCP, PARENT copy
        assertEquals(SessionAuthType.PARENT, result.get(1).toolConfig().getSessionAuth());
        assertTrue(result.get(1).toolConfig() instanceof McpExpandedToolDTO);
        assertEquals("mcp-cfg_toolA", result.get(1).toolConfig().getName());
        // result[2]: mcp-cfg, CHILD copy of raw config (not expanded)
        assertEquals(SessionAuthType.CHILD, result.get(2).toolConfig().getSessionAuth());
        assertFalse(result.get(2).toolConfig() instanceof McpExpandedToolDTO);
        assertEquals("mcp-cfg", result.get(2).toolConfig().getName());
        assertNull(result.get(2).invoker());
        assertSame(result.get(2).toolConfig(), result.get(2).mcpOriginalConfig());
    }

    @Test
    void CUSTOM类型provider正常时返回对应invoker() {
        Long sessionId = 600L;
        Long toolId = 60L;
        ToolConfigDTO dto = ToolConfigDTO.builder()
                .id(toolId).name("custom-tool").toolType(ToolType.CUSTOM).implPath("my.CustomImpl")
                .sessionAuth(SessionAuthType.ALL).build();

        CustomToolInvokerProvider provider = tc -> {
            assertEquals(dto, tc);
            return new CustomToolInvoker(tc) {
                @Override
                public String execute(com.ghost616.agentbase.service.agent.AgentExecutionContext ctx, String arguments) {
                    return "custom-result";
                }
            };
        };
        when(registry.getCustomToolInvokerProvider()).thenReturn(provider);
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
        Long sessionId = 700L;
        Long toolId = 70L;
        ToolConfigDTO dto = ToolConfigDTO.builder()
                .id(toolId).name("custom-null").toolType(ToolType.CUSTOM).implPath("no.Provider")
                .sessionAuth(SessionAuthType.ALL).build();

        when(registry.getCustomToolInvokerProvider()).thenReturn(null);
        when(dataProvider.getSessionToolIds(sessionId))
                .thenReturn(List.of(new SessionToolInfo(toolId, SessionAuthType.ALL)));
        when(dataProvider.getToolById(toolId)).thenReturn(dto);

        assertThrows(UnsupportedOperationException.class,
                () -> toolManager.getSessionTools(sessionId, false));
    }
}
