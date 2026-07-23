package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvokerProvider;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class DefaultCustomToolInvokerProviderTest {

    @Test
    void 应实现CustomToolInvokerProvider接口() {
        assertTrue(CustomToolInvokerProvider.class.isAssignableFrom(DefaultCustomToolInvokerProvider.class));
    }

    @Test
    void getInvoker应抛出UnsupportedOperationException() {
        DefaultCustomToolInvokerProvider provider = new DefaultCustomToolInvokerProvider();
        ToolConfigDTO config = ToolConfigDTO.builder()
                .id(1L).name("test").toolType(ToolType.CUSTOM).implPath("test.Test").build();
        assertThrows(UnsupportedOperationException.class, () -> provider.getInvoker(config));
    }
}
