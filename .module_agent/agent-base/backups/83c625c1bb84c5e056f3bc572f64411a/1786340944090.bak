package com.ghost616.agentbase.dto.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatChunkTest {

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
                .finishReason("stop")
                .usage(usage)
                .build();

        assertNotNull(chunk.getUsage());
        assertEquals(100, chunk.getUsage().getPromptTokens());
        assertEquals(200, chunk.getUsage().getCompletionTokens());
        assertEquals(300, chunk.getUsage().getTotalTokens());
        assertEquals("Hello", chunk.getDelta());
        assertEquals("stop", chunk.getFinishReason());
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
}
