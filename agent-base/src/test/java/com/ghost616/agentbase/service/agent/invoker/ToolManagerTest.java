package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.dto.tool.McpExpandedToolDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolManagerTest {

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
}
