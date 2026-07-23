package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import static org.junit.jupiter.api.Assertions.*;

class CustomToolInvokerTest {

    @Test
    void CustomToolInvoker应声明为抽象类() {
        assertTrue(Modifier.isAbstract(CustomToolInvoker.class.getModifiers()));
    }

    @Test
    void CustomToolInvoker应实现ToolInvoker接口() {
        assertTrue(ToolInvoker.class.isAssignableFrom(CustomToolInvoker.class));
    }

    @Test
    void CustomToolInvoker应包含protected_final_ToolConfigDTO字段() throws Exception {
        var field = CustomToolInvoker.class.getDeclaredField("toolConfig");
        assertTrue(Modifier.isProtected(field.getModifiers()));
        assertTrue(Modifier.isFinal(field.getModifiers()));
        assertEquals(ToolConfigDTO.class, field.getType());
    }

    @Test
    void CustomToolInvoker构造函数应注入ToolConfigDTO() throws Exception {
        Constructor<?> constructor = CustomToolInvoker.class.getDeclaredConstructor(ToolConfigDTO.class);
        assertTrue(Modifier.isProtected(constructor.getModifiers()));
    }
}
