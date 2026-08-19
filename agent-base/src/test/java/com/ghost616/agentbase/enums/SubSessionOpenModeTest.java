package com.ghost616.agentbase.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class SubSessionOpenModeTest {

    @Test
    void 枚举应包含两个值() {
        SubSessionOpenMode[] values = SubSessionOpenMode.values();
        assertEquals(2, values.length);
        assertNotNull(SubSessionOpenMode.WEBSOCKET);
        assertNotNull(SubSessionOpenMode.TOOL_CALL);
    }

    @Test
    void WEBSOCKET的code与description应正确() {
        assertEquals("WEBSOCKET", SubSessionOpenMode.WEBSOCKET.getCode());
        assertEquals("WebSocket推送", SubSessionOpenMode.WEBSOCKET.getDescription());
    }

    @Test
    void TOOL_CALL的code与description应正确() {
        assertEquals("TOOL_CALL", SubSessionOpenMode.TOOL_CALL.getCode());
        assertEquals("前台工具调用", SubSessionOpenMode.TOOL_CALL.getDescription());
    }

    @Test
    void DEFAULT应指向TOOL_CALL() {
        assertSame(SubSessionOpenMode.TOOL_CALL, SubSessionOpenMode.DEFAULT);
        assertEquals("TOOL_CALL", SubSessionOpenMode.DEFAULT.getCode());
    }

    @Test
    void code字段应标注EnumValue注解() throws Exception {
        Field codeField = SubSessionOpenMode.class.getDeclaredField("code");
        assertNotNull(codeField.getAnnotation(EnumValue.class),
                "code 字段必须标注 @EnumValue 注解（供 MyBatis-Plus 枚举映射使用）");
    }

    @Test
    void 按name查找应能得到对应枚举() {
        for (SubSessionOpenMode mode : SubSessionOpenMode.values()) {
            assertEquals(mode, SubSessionOpenMode.valueOf(mode.name()));
        }
    }
}