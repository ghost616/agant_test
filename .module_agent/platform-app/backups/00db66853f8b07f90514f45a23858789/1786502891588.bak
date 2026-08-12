package com.ghost616.platform.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SessionMemoryDocumentTest {

    @Test
    void builder_shouldSetAllFields() {
        SessionMemoryDocument doc = SessionMemoryDocument.builder()
                .sessionId("100")
                .aggregationStartSeq(1)
                .aggregationEndSeq(5)
                .aggregationText("用户咨询了登录流程")
                .vector(List.of(0.1f, 0.2f))
                .build();

        assertEquals("100", doc.getSessionId());
        assertEquals(1, doc.getAggregationStartSeq());
        assertEquals(5, doc.getAggregationEndSeq());
        assertEquals("用户咨询了登录流程", doc.getAggregationText());
        assertEquals(2, doc.getVector().size());
    }

    @Test
    void noArgsConstructor_shouldLeaveFieldsNull() {
        SessionMemoryDocument doc = new SessionMemoryDocument();

        assertNull(doc.getSessionId());
        assertNull(doc.getAggregationStartSeq());
        assertNull(doc.getAggregationEndSeq());
        assertNull(doc.getAggregationText());
        assertNull(doc.getVector());
    }

    @Test
    void allArgsConstructor_shouldPopulateFields() {
        SessionMemoryDocument doc = new SessionMemoryDocument("200", 3, 8, "摘要", List.of(0.5f));

        assertEquals("200", doc.getSessionId());
        assertEquals(3, doc.getAggregationStartSeq());
        assertEquals(8, doc.getAggregationEndSeq());
        assertEquals("摘要", doc.getAggregationText());
        assertNotNull(doc.getVector());
    }
}
