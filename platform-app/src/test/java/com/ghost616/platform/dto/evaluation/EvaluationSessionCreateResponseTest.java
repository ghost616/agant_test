package com.ghost616.platform.dto.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationSessionCreateResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void builderShouldCreateResponseWithAllFields() {
        EvaluationSessionCreateResponse response = EvaluationSessionCreateResponse.builder()
                .sessionId(100L)
                .userMessages(List.of("msg1", "msg2"))
                .build();

        assertEquals(100L, response.getSessionId());
        assertEquals(List.of("msg1", "msg2"), response.getUserMessages());
    }

    @Test
    void noArgsConstructorShouldCreateEmptyResponse() {
        EvaluationSessionCreateResponse response = new EvaluationSessionCreateResponse();
        assertNull(response.getSessionId());
        assertNull(response.getUserMessages());
    }

    @Test
    void allArgsConstructorShouldSetAllFields() {
        EvaluationSessionCreateResponse response = new EvaluationSessionCreateResponse(100L, List.of("hello"));
        assertEquals(100L, response.getSessionId());
        assertEquals(List.of("hello"), response.getUserMessages());
    }

    @Test
    void setterShouldUpdateFields() {
        EvaluationSessionCreateResponse response = new EvaluationSessionCreateResponse();
        response.setSessionId(200L);
        response.setUserMessages(List.of("a", "b", "c"));

        assertEquals(200L, response.getSessionId());
        assertEquals(List.of("a", "b", "c"), response.getUserMessages());
    }

    @Test
    void jsonSerializationShouldOutputLongFieldAsString() throws Exception {
        EvaluationSessionCreateResponse response = EvaluationSessionCreateResponse.builder()
                .sessionId(123L)
                .userMessages(List.of("test"))
                .build();

        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"sessionId\":\"123\""));
        assertTrue(json.contains("\"userMessages\":[\"test\"]"));
    }

    @Test
    void jsonDeserializationShouldParseStringLongField() throws Exception {
        String json = "{\"sessionId\":\"123\",\"userMessages\":[\"hello\",\"world\"]}";
        EvaluationSessionCreateResponse response = objectMapper.readValue(json, EvaluationSessionCreateResponse.class);
        assertEquals(123L, response.getSessionId());
        assertEquals(List.of("hello", "world"), response.getUserMessages());
    }
}
