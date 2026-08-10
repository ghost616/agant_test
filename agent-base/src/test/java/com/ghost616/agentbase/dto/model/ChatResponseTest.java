package com.ghost616.agentbase.dto.model;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.enums.FinishReason;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatResponseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void builderShouldSetFinishReason() {
        ChatResponse response = ChatResponse.builder()
                .content("hi")
                .finishReason(FinishReason.LENGTH)
                .build();

        assertEquals(FinishReason.LENGTH, response.getFinishReason());
    }

    @Test
    void finishReasonShouldBeNullByDefault() {
        ChatResponse response = new ChatResponse();
        assertNull(response.getFinishReason());
    }

    @Test
    void setterAndGetterShouldWork() {
        ChatResponse response = new ChatResponse();
        response.setFinishReason(FinishReason.TOOL_CALLS);
        assertEquals(FinishReason.TOOL_CALLS, response.getFinishReason());
    }

    @Test
    void finishReason字段序列化应输出小写code() throws Exception {
        ChatResponse response = ChatResponse.builder().finishReason(FinishReason.STOP).build();
        String json = OBJECT_MAPPER.writeValueAsString(response);
        assertTrue(json.contains("\"finishReason\":\"stop\""), "实际序列化结果: " + json);

        ChatResponse errorResponse = ChatResponse.builder().finishReason(FinishReason.ERROR).build();
        String errorJson = OBJECT_MAPPER.writeValueAsString(errorResponse);
        assertTrue(errorJson.contains("\"finishReason\":\"error\""), "实际序列化结果: " + errorJson);
    }
}
