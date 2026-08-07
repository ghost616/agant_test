package com.ghost616.platform.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubToolTypeTest {

    @Test
    void enum_shouldContainBROWSER() {
        assertEquals(2, SubToolType.values().length);
        assertEquals("BROWSER", SubToolType.BROWSER.name());
        assertEquals("RAG_KNOWLEDGE", SubToolType.RAG_KNOWLEDGE.name());
    }

    @Test
    void getCode_shouldReturnBROWSER() {
        assertEquals("BROWSER", SubToolType.BROWSER.getCode());
    }

    @Test
    void getDescription_shouldReturnChinese() {
        assertEquals("浏览器", SubToolType.BROWSER.getDescription());
    }
}
