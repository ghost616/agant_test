package com.ghost616.agentbase.enums;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinishReasonTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void 六个枚举值应齐全() {
        assertNotNull(FinishReason.STOP);
        assertNotNull(FinishReason.LENGTH);
        assertNotNull(FinishReason.TOOL_CALLS);
        assertNotNull(FinishReason.CONTENT_FILTER);
        assertNotNull(FinishReason.ERROR);
        assertNotNull(FinishReason.CANCELLED);
        assertEquals(6, FinishReason.values().length);
    }

    @Test
    void STOP的getCode应返回stop() {
        assertEquals("stop", FinishReason.STOP.getCode());
    }

    @Test
    void STOP的getDescription应返回正常结束() {
        assertEquals("正常结束", FinishReason.STOP.getDescription());
    }

    @Test
    void LENGTH的getCode应返回length() {
        assertEquals("length", FinishReason.LENGTH.getCode());
    }

    @Test
    void LENGTH的getDescription应返回达到长度限制() {
        assertEquals("达到长度限制", FinishReason.LENGTH.getDescription());
    }

    @Test
    void TOOL_CALLS的getCode应返回tool_calls() {
        assertEquals("tool_calls", FinishReason.TOOL_CALLS.getCode());
    }

    @Test
    void TOOL_CALLS的getDescription应返回触发工具调用() {
        assertEquals("触发工具调用", FinishReason.TOOL_CALLS.getDescription());
    }

    @Test
    void CONTENT_FILTER的getCode应返回content_filter() {
        assertEquals("content_filter", FinishReason.CONTENT_FILTER.getCode());
    }

    @Test
    void CONTENT_FILTER的getDescription应返回内容被过滤() {
        assertEquals("内容被过滤", FinishReason.CONTENT_FILTER.getDescription());
    }

    @Test
    void ERROR的getCode应返回error() {
        assertEquals("error", FinishReason.ERROR.getCode());
    }

    @Test
    void ERROR的getDescription应返回发生错误() {
        assertEquals("发生错误", FinishReason.ERROR.getDescription());
    }

    @Test
    void CANCELLED的getCode应返回cancelled() {
        assertEquals("cancelled", FinishReason.CANCELLED.getCode());
    }

    @Test
    void CANCELLED的getDescription应返回被取消() {
        assertEquals("被取消", FinishReason.CANCELLED.getDescription());
    }

    @Test
    void fromCode对已知值应返回对应枚举() {
        assertEquals(FinishReason.STOP, FinishReason.fromCode("stop"));
        assertEquals(FinishReason.LENGTH, FinishReason.fromCode("length"));
        assertEquals(FinishReason.TOOL_CALLS, FinishReason.fromCode("tool_calls"));
        assertEquals(FinishReason.CONTENT_FILTER, FinishReason.fromCode("content_filter"));
        assertEquals(FinishReason.ERROR, FinishReason.fromCode("error"));
        assertEquals(FinishReason.CANCELLED, FinishReason.fromCode("cancelled"));
    }

    @Test
    void fromCode对未知值应返回null() {
        assertNull(FinishReason.fromCode("unknown"));
        assertNull(FinishReason.fromCode(""));
    }

    @Test
    void fromCode对null应返回null() {
        assertNull(FinishReason.fromCode(null));
    }

    @Test
    void fromCode返回值与getCode应互相可逆() {
        for (FinishReason reason : FinishReason.values()) {
            assertEquals(reason, FinishReason.fromCode(reason.getCode()));
        }
        assertTrue(FinishReason.fromCode("stop") instanceof FinishReason);
        assertFalse(FinishReason.fromCode("bad") instanceof FinishReason);
    }

    @Test
    void 枚举序列化应输出小写code而非大写枚举名() throws Exception {
        assertEquals("\"stop\"", OBJECT_MAPPER.writeValueAsString(FinishReason.STOP));
        assertEquals("\"length\"", OBJECT_MAPPER.writeValueAsString(FinishReason.LENGTH));
        assertEquals("\"tool_calls\"", OBJECT_MAPPER.writeValueAsString(FinishReason.TOOL_CALLS));
        assertEquals("\"content_filter\"", OBJECT_MAPPER.writeValueAsString(FinishReason.CONTENT_FILTER));
        assertEquals("\"error\"", OBJECT_MAPPER.writeValueAsString(FinishReason.ERROR));
        assertEquals("\"cancelled\"", OBJECT_MAPPER.writeValueAsString(FinishReason.CANCELLED));
    }

    @Test
    void 枚举反序列化应按小写code恢复枚举() throws Exception {
        assertEquals(FinishReason.STOP, OBJECT_MAPPER.readValue("\"stop\"", FinishReason.class));
        assertEquals(FinishReason.LENGTH, OBJECT_MAPPER.readValue("\"length\"", FinishReason.class));
        assertEquals(FinishReason.TOOL_CALLS, OBJECT_MAPPER.readValue("\"tool_calls\"", FinishReason.class));
        assertEquals(FinishReason.CONTENT_FILTER, OBJECT_MAPPER.readValue("\"content_filter\"", FinishReason.class));
        assertEquals(FinishReason.ERROR, OBJECT_MAPPER.readValue("\"error\"", FinishReason.class));
        assertEquals(FinishReason.CANCELLED, OBJECT_MAPPER.readValue("\"cancelled\"", FinishReason.class));
    }
}
