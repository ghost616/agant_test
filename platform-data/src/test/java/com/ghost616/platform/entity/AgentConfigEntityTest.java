package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ghost616.agentbase.enums.SubSessionOpenMode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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

    @Test
    void vectorModelIdField() throws Exception {
        Field field = AgentConfig.class.getDeclaredField("vectorModelId");
        assertNotNull(field);
        assertEquals(Long.class, field.getType());
        assertEquals("vector_model_id", field.getAnnotation(TableField.class).value());

        AgentConfig config = new AgentConfig();
        config.setVectorModelId(100L);
        assertEquals(100L, config.getVectorModelId());
    }

    @Test
    void subSessionOpenModeField() throws Exception {
        Field field = AgentConfig.class.getDeclaredField("subSessionOpenMode");
        assertNotNull(field);
        assertEquals(SubSessionOpenMode.class, field.getType());
        assertEquals("sub_session_open_mode", field.getAnnotation(TableField.class).value());

        AgentConfig config = new AgentConfig();
        assertSame(SubSessionOpenMode.DEFAULT, config.getSubSessionOpenMode(),
                "subSessionOpenMode default value should be SubSessionOpenMode.DEFAULT (TOOL_CALL)");
        assertEquals(SubSessionOpenMode.TOOL_CALL, config.getSubSessionOpenMode());

        config.setSubSessionOpenMode(SubSessionOpenMode.WEBSOCKET);
        assertEquals(SubSessionOpenMode.WEBSOCKET, config.getSubSessionOpenMode());
    }
}
