package com.ghost616.platform.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AggregationTypeTest {

    @Test
    void enumCountAndValues() {
        AggregationType[] values = AggregationType.values();
        assertEquals(2, values.length);
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("GROUP", "分组聚合");
        expected.put("DAILY", "按日聚合");
        assertEquals(expected.keySet().size(), values.length);
        for (AggregationType v : values) {
            assertEquals(v.name(), v.getCode(), "code should equal enum name for " + v.name());
            assertEquals(expected.get(v.name()), v.getDescription(), "description mismatch for " + v.name());
        }
    }

    @Test
    void enumValueAnnotationOnCodeField() throws Exception {
        Field codeField = AggregationType.class.getDeclaredField("code");
        assertNotNull(codeField.getAnnotation(EnumValue.class), "code field should be annotated with @EnumValue");
        assertEquals(String.class, codeField.getType());
    }
}
