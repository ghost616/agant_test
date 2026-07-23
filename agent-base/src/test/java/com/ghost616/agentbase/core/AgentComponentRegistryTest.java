package com.ghost616.agentbase.core;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvokerProvider;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class AgentComponentRegistryTest {

    @Test
    void customToolInvokerProvider字段应存在() throws Exception {
        Field field = AgentComponentRegistry.class.getDeclaredField("customToolInvokerProvider");
        assertEquals(CustomToolInvokerProvider.class, field.getType());
    }

    @Test
    void customToolInvokerProvider应有setter方法() throws Exception {
        Method setter = AgentComponentRegistry.class.getDeclaredMethod("setCustomToolInvokerProvider", CustomToolInvokerProvider.class);
        assertNotNull(setter);
    }

    @Test
    void getCustomToolInvokerProvider应返回对应字段() throws Exception {
        Method method = AgentComponentRegistry.class.getDeclaredMethod("getCustomToolInvokerProvider");
        assertEquals(CustomToolInvokerProvider.class, method.getReturnType());
    }

    @Test
    void getCustomToolInvokerProvider未设置时应返回null() {
        AgentComponentRegistry registry = new AgentComponentRegistry();
        assertNull(registry.getCustomToolInvokerProvider());
    }

    @Test
    void getCustomToolInvokerProvider设置后应正确返回() {
        AgentComponentRegistry registry = new AgentComponentRegistry();
        CustomToolInvokerProvider provider = tc -> new CustomToolInvoker(tc) {
            @Override
            public String execute(com.ghost616.agentbase.service.agent.AgentExecutionContext ctx, String arguments) {
                return "test";
            }
        };
        registry.setCustomToolInvokerProvider(provider);
        assertSame(provider, registry.getCustomToolInvokerProvider());
    }
}
