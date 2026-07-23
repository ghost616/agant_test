package com.ghost616.agentbase.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolTypeTest {

    @Test
    void CUSTOM枚举应存在() {
        assertNotNull(ToolType.CUSTOM);
    }

    @Test
    void CUSTOM的getCode应返回CUSTOM() {
        assertEquals("CUSTOM", ToolType.CUSTOM.getCode());
    }

    @Test
    void CUSTOM的getDescription应返回自定义实现() {
        assertEquals("自定义实现", ToolType.CUSTOM.getDescription());
    }
}
