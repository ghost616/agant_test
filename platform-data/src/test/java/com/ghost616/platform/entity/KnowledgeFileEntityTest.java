package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ghost616.platform.enums.PublishStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KnowledgeFileEntityTest {

    @Test
    void publishStatusFieldExistsWithTableFieldAnnotation() throws Exception {
        Field field = KnowledgeFile.class.getDeclaredField("publishStatus");
        assertNotNull(field);
        assertEquals(PublishStatus.class, field.getType(), "publishStatus field type should be PublishStatus");
        TableField annotation = field.getAnnotation(TableField.class);
        assertNotNull(annotation, "publishStatus field should be annotated with @TableField");
        assertEquals("publish_status", annotation.value(), "@TableField value should be publish_status");
    }

    @Test
    void classIsNotAbstractAndHasTableNameAnnotation() {
        assertNotNull(KnowledgeFile.class.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class));
        assertEquals(false, Modifier.isAbstract(KnowledgeFile.class.getModifiers()));
    }
}
