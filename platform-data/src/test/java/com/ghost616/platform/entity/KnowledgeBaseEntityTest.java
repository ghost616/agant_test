package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KnowledgeBaseEntityTest {

    @Test
    void vectorModelIdField() throws Exception {
        Field field = KnowledgeBase.class.getDeclaredField("vectorModelId");
        assertNotNull(field);
        assertEquals(Long.class, field.getType());
        assertEquals("vector_model_id", field.getAnnotation(TableField.class).value());
    }

    @Test
    void esIndexField() throws Exception {
        Field field = KnowledgeBase.class.getDeclaredField("esIndex");
        assertNotNull(field);
        assertEquals(String.class, field.getType());
        assertEquals("es_index", field.getAnnotation(TableField.class).value());
    }

    @Test
    void rebuildingFieldDefaultFalse() throws Exception {
        Field field = KnowledgeBase.class.getDeclaredField("rebuilding");
        assertNotNull(field);
        assertEquals(Boolean.class, field.getType());
        assertEquals("rebuilding", field.getAnnotation(TableField.class).value());
        boolean hasInitializer = false;
        KnowledgeBase kb = new KnowledgeBase();
        assertFalse(kb.getRebuilding(), "rebuilding default value should be false");
    }

    @Test
    void classNotAbstract() {
        assertFalse(Modifier.isAbstract(KnowledgeBase.class.getModifiers()));
    }
}
