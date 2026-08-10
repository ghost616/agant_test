package com.ghost616.agentbase.dto.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.enums.FinishReason;

class ChatChunkTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void setAndGetUsage() {
        UsageInfo usage = UsageInfo.builder()
                .promptTokens(10)
                .completionTokens(20)
                .totalTokens(30)
                .build();

        ChatChunk chunk = new ChatChunk();
        chunk.setUsage(usage);

        assertNotNull(chunk.getUsage());
        assertEquals(10, chunk.getUsage().getPromptTokens());
        assertEquals(20, chunk.getUsage().getCompletionTokens());
        assertEquals(30, chunk.getUsage().getTotalTokens());
    }

    @Test
    void buildWithUsage() {
        UsageInfo usage = UsageInfo.builder()
                .promptTokens(100)
                .completionTokens(200)
                .totalTokens(300)
                .build();

        ChatChunk chunk = ChatChunk.builder()
                .delta("Hello")
                .finishReason(FinishReason.STOP)
                .usage(usage)
                .build();

        assertNotNull(chunk.getUsage());
        assertEquals(100, chunk.getUsage().getPromptTokens());
        assertEquals(200, chunk.getUsage().getCompletionTokens());
        assertEquals(300, chunk.getUsage().getTotalTokens());
        assertEquals("Hello", chunk.getDelta());
        assertEquals(FinishReason.STOP, chunk.getFinishReason());
    }

    @Test
    void usageIsNullByDefault() {
        ChatChunk chunk = new ChatChunk();
        assertNull(chunk.getUsage());
    }

    @Test
    void buildWithoutUsage() {
        ChatChunk chunk = ChatChunk.builder()
                .delta("test")
                .build();

        assertNull(chunk.getUsage());
        assertEquals("test", chunk.getDelta());
    }

    @Test
    void usageFieldWithPartialValues() {
        UsageInfo usage = UsageInfo.builder()
                .promptTokens(50)
                .build();

        ChatChunk chunk = ChatChunk.builder().usage(usage).build();
        assertNotNull(chunk.getUsage());
        assertEquals(50, chunk.getUsage().getPromptTokens());
        assertNull(chunk.getUsage().getCompletionTokens());
        assertNull(chunk.getUsage().getTotalTokens());
    }

    @Test
    void finishReason字段序列化应输出小写code() throws Exception {
        ChatChunk chunk = ChatChunk.builder().delta("hi").finishReason(FinishReason.STOP).build();
        String json = OBJECT_MAPPER.writeValueAsString(chunk);
        assertTrue(json.contains("\"finishReason\":\"stop\""), "实际序列化结果: " + json);

        ChatChunk errorChunk = ChatChunk.builder().finishReason(FinishReason.ERROR).build();
        String errorJson = OBJECT_MAPPER.writeValueAsString(errorChunk);
        assertTrue(errorJson.contains("\"finishReason\":\"error\""), "实际序列化结果: " + errorJson);
    }
}
