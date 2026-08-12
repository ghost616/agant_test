package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentConfigEntityTest {

    @Test
    void memoryEnabledField() throws Exception {
        Field field = AgentConfig.class.getDeclaredField("memoryEnabled");
        assertNotNull(field);
        assertEquals(Boolean.class, field.getType());
        assertEquals("memory_enabled", field.getAnnotation(TableField.class).value());

        AgentConfig config = new AgentConfig();
        assertFalse(config.getMemoryEnabled(), "memoryEnabled default value should be false");
    }

    @Test
    void memoryGroupCountField() throws Exception {
        Field field = AgentConfig.class.getDeclaredField("memoryGroupCount");
        assertNotNull(field);
        assertEquals(Integer.class, field.getType());
        assertEquals("memory_group_count", field.getAnnotation(TableField.class).value());

        AgentConfig config = new AgentConfig();
        assertEquals(30, config.getMemoryGroupCount(), "memoryGroupCount default value should be 30");
    }
}
