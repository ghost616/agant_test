package com.ghost616.platform.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublishStatusTest {

    @Test
    void enumCountAndValues() {
        PublishStatus[] values = PublishStatus.values();
        assertEquals(5, values.length);
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("UNPUBLISHED", "未发布");
        expected.put("PUBLISHING", "正在发布");
        expected.put("PUBLISHED", "已发布");
        expected.put("PENDING_PUBLISH", "待发布（已发布后内容被修改）");
        expected.put("PUBLISH_ERROR", "发布错误");
        assertEquals(expected.keySet().size(), values.length);
        for (PublishStatus v : values) {
            assertEquals(v.name(), v.getCode(), "code should equal enum name for " + v.name());
            assertEquals(expected.get(v.name()), v.getDescription(), "description mismatch for " + v.name());
        }
    }

    @Test
    void enumValueAnnotationOnCodeField() throws Exception {
        Field codeField = PublishStatus.class.getDeclaredField("code");
        assertNotNull(codeField.getAnnotation(EnumValue.class), "code field should be annotated with @EnumValue");
        assertEquals(String.class, codeField.getType());
    }
}
