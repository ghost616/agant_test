package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AgentLogEntityTest {

    @Test
    void classTableNameMapsToAgentLog() {
        TableName tableName = AgentLogEntity.class.getAnnotation(TableName.class);
        assertNotNull(tableName);
        assertEquals("agent_log", tableName.value());
    }

    @Test
    void sessionVariablesField() throws Exception {
        Field field = AgentLogEntity.class.getDeclaredField("sessionVariables");
        assertNotNull(field);
        assertEquals(String.class, field.getType());
        TableField annotation = field.getAnnotation(TableField.class);
        assertNotNull(annotation, "sessionVariables should have @TableField");
        assertEquals("session_variables", annotation.value());
    }

    @Test
    void conversationVariablesField() throws Exception {
        Field field = AgentLogEntity.class.getDeclaredField("conversationVariables");
        assertNotNull(field);
        assertEquals(String.class, field.getType());
        TableField annotation = field.getAnnotation(TableField.class);
        assertNotNull(annotation, "conversationVariables should have @TableField");
        assertEquals("conversation_variables", annotation.value());
    }

    @Test
    void lombokGetterSetterRoundTrip() {
        AgentLogEntity entity = new AgentLogEntity();
        assertNull(entity.getSessionVariables());
        assertNull(entity.getConversationVariables());

        entity.setSessionVariables("{\"k\":\"v\"}");
        entity.setConversationVariables("{\"a\":1}");

        assertEquals("{\"k\":\"v\"}", entity.getSessionVariables());
        assertEquals("{\"a\":1}", entity.getConversationVariables());
    }
}
