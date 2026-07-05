package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.dto.model.ToolDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemToolManagerTest {

    @Mock
    private SystemToolProvider provider;

    @Mock
    private SystemTool toolA;

    @Mock
    private SystemTool toolB;

    @Mock
    private SystemTool blankNameTool;

    @Mock
    private SystemTool nullNameTool;

    @Test
    void constructor调用provider_discoverSystemTools并注册工具() {
        when(toolA.getToolName()).thenReturn("tool_a");
        when(toolB.getToolName()).thenReturn("tool_b");
        when(provider.discoverSystemTools()).thenReturn(Map.of("tool_a", toolA, "tool_b", toolB));

        SystemToolManager manager = new SystemToolManager(provider);

        verify(provider, times(1)).discoverSystemTools();
        assertSame(toolA, manager.getSystemTool("tool_a"));
        assertSame(toolB, manager.getSystemTool("tool_b"));
    }

    @Test
    void 空白toolName的工具被跳过不注册() {
        when(toolA.getToolName()).thenReturn("tool_a");
        when(blankNameTool.getToolName()).thenReturn("  ");
        when(provider.discoverSystemTools()).thenReturn(Map.of("tool_a", toolA, "blank", blankNameTool));

        SystemToolManager manager = new SystemToolManager(provider);

        assertNotNull(manager.getSystemTool("tool_a"));
        assertNull(manager.getSystemTool("blank"));
    }

    @Test
    void nullToolName的工具被跳过不注册() {
        when(toolA.getToolName()).thenReturn("tool_a");
        when(nullNameTool.getToolName()).thenReturn(null);
        when(provider.discoverSystemTools()).thenReturn(Map.of("tool_a", toolA, "null_tool", nullNameTool));

        SystemToolManager manager = new SystemToolManager(provider);

        assertNotNull(manager.getSystemTool("tool_a"));
        assertNull(manager.getSystemTool("null_tool"));
    }

    @Test
    void getSystemTool返回已注册的工具() {
        when(toolA.getToolName()).thenReturn("tool_a");
        when(provider.discoverSystemTools()).thenReturn(Map.of("tool_a", toolA));

        SystemToolManager manager = new SystemToolManager(provider);

        assertSame(toolA, manager.getSystemTool("tool_a"));
    }

    @Test
    void getSystemTool不存在的工具返回null() {
        when(toolA.getToolName()).thenReturn("tool_a");
        when(provider.discoverSystemTools()).thenReturn(Map.of("tool_a", toolA));

        SystemToolManager manager = new SystemToolManager(provider);

        assertNull(manager.getSystemTool("nonexistent"));
    }

    @Test
    void getToolDefinitions返回带_sys_前缀的ToolDefinition列表() {
        when(toolA.getToolName()).thenReturn("tool_a");
        when(toolA.getDescription()).thenReturn("Tool A");
        when(toolA.getParameterSchema()).thenReturn("{\"type\":\"object\"}");
        when(toolB.getToolName()).thenReturn("tool_b");
        when(toolB.getDescription()).thenReturn("Tool B");
        when(toolB.getParameterSchema()).thenReturn("{\"type\":\"object\"}");
        when(provider.discoverSystemTools()).thenReturn(Map.of("tool_a", toolA, "tool_b", toolB));

        SystemToolManager manager = new SystemToolManager(provider);

        List<ToolDefinition> defs = manager.getToolDefinitions();

        assertEquals(2, defs.size());
        assertTrue(defs.stream().anyMatch(d -> "_sys_tool_a".equals(d.getName())));
        assertTrue(defs.stream().anyMatch(d -> "_sys_tool_b".equals(d.getName())));
    }

    @Test
    void getToolDefinitions跳过空名称工具() {
        when(toolA.getToolName()).thenReturn("tool_a");
        when(toolA.getDescription()).thenReturn("Tool A");
        when(toolA.getParameterSchema()).thenReturn("{}");
        when(blankNameTool.getToolName()).thenReturn("  ");
        when(nullNameTool.getToolName()).thenReturn(null);
        when(provider.discoverSystemTools()).thenReturn(Map.of(
                "tool_a", toolA, "blank", blankNameTool, "null_tool", nullNameTool
        ));

        SystemToolManager manager = new SystemToolManager(provider);

        List<ToolDefinition> defs = manager.getToolDefinitions();
        assertEquals(1, defs.size());
        assertEquals("_sys_tool_a", defs.get(0).getName());
    }

    @Test
    void provider返回空Map时无工具注册() {
        when(provider.discoverSystemTools()).thenReturn(Map.of());

        SystemToolManager manager = new SystemToolManager(provider);

        assertNull(manager.getSystemTool("anything"));
        assertTrue(manager.getToolDefinitions().isEmpty());
    }
}
