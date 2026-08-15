package com.ghost616.platform.model;

import com.ghost616.platform.enums.AggregationType;
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
                .aggregationType(AggregationType.GROUP)
                .aggregationStartSeq(1)
                .aggregationEndSeq(5)
                .aggregationStartTime(1000L)
                .aggregationEndTime(5000L)
                .aggregationText("用户咨询了登录流程")
                .vector(List.of(0.1f, 0.2f))
                .build();

        assertEquals("100", doc.getSessionId());
        assertEquals(AggregationType.GROUP, doc.getAggregationType());
        assertEquals(1, doc.getAggregationStartSeq());
        assertEquals(5, doc.getAggregationEndSeq());
        assertEquals(1000L, doc.getAggregationStartTime());
        assertEquals(5000L, doc.getAggregationEndTime());
        assertEquals("用户咨询了登录流程", doc.getAggregationText());
        assertEquals(2, doc.getVector().size());
    }

    @Test
    void noArgsConstructor_shouldLeaveFieldsNull() {
        SessionMemoryDocument doc = new SessionMemoryDocument();

        assertNull(doc.getSessionId());
        assertNull(doc.getAggregationType());
        assertNull(doc.getAggregationStartSeq());
        assertNull(doc.getAggregationEndSeq());
        assertNull(doc.getAggregationStartTime());
        assertNull(doc.getAggregationEndTime());
        assertNull(doc.getAggregationText());
        assertNull(doc.getVector());
    }

    @Test
    void allArgsConstructor_shouldPopulateFields() {
        SessionMemoryDocument doc = new SessionMemoryDocument(
                "200", 42L, AggregationType.DAILY, 3, 8, 3000L, 8000L, "摘要", List.of(0.5f));

        assertEquals("200", doc.getSessionId());
        assertEquals(42L, doc.getUserId());
        assertEquals(AggregationType.DAILY, doc.getAggregationType());
        assertEquals(3, doc.getAggregationStartSeq());
        assertEquals(8, doc.getAggregationEndSeq());
        assertEquals(3000L, doc.getAggregationStartTime());
        assertEquals(8000L, doc.getAggregationEndTime());
        assertEquals("摘要", doc.getAggregationText());
        assertNotNull(doc.getVector());
    }
}
