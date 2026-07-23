package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class CustomToolInvokerProviderTest {

    @Test
    void CustomToolInvokerProvider应声明为接口() {
        assertTrue(CustomToolInvokerProvider.class.isInterface());
    }

    @Test
    void CustomToolInvokerProvider应定义getInvoker方法() throws Exception {
        Method method = CustomToolInvokerProvider.class.getDeclaredMethod("getInvoker", ToolConfigDTO.class);
        assertNotNull(method);
        assertEquals(CustomToolInvoker.class, method.getReturnType());
    }
}
