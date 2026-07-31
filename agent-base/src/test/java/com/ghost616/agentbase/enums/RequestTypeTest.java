package com.ghost616.agentbase.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequestTypeTest {

    @Test
    void RESPONSES枚举应存在() {
        assertNotNull(RequestType.RESPONSES);
        assertNotNull(RequestType.RESPONSES_STATELESS);
        assertNotNull(RequestType.COMPLETIONS);
    }

    @Test
    void RESPONSES的getCode应返回responses() {
        assertEquals("responses", RequestType.RESPONSES.getCode());
    }

    @Test
    void RESPONSES的getDescription应返回有状态说明() {
        assertEquals("Responses（有状态）", RequestType.RESPONSES.getDescription());
    }

    @Test
    void RESPONSES_STATELESS的getCode应返回responses_stateless() {
        assertEquals("responses_stateless", RequestType.RESPONSES_STATELESS.getCode());
    }

    @Test
    void RESPONSES_STATELESS的getDescription应返回无状态说明() {
        assertEquals("Responses（无状态）", RequestType.RESPONSES_STATELESS.getDescription());
    }

    @Test
    void COMPLETIONS的getCode应返回completions() {
        assertEquals("completions", RequestType.COMPLETIONS.getCode());
    }

    @Test
    void COMPLETIONS的getDescription应返回ChatCompletions说明() {
        assertEquals("Chat Completions", RequestType.COMPLETIONS.getDescription());
    }

    @Test
    void isResponses对responses应返回true() {
        assertTrue(RequestType.isResponses("responses"));
    }

    @Test
    void isResponses对responses_stateless应返回true() {
        assertTrue(RequestType.isResponses("responses_stateless"));
    }

    @Test
    void isResponses对completions应返回false() {
        assertFalse(RequestType.isResponses(RequestType.COMPLETIONS.getCode()));
    }

    @Test
    void isResponses对其他值应返回false() {
        assertFalse(RequestType.isResponses("openai"));
        assertFalse(RequestType.isResponses(""));
        assertFalse(RequestType.isResponses(null));
    }
}
