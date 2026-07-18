package com.ghost616.agentinteg.model.invoker;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.model.UsageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OpenAIInvokerTest {

    private OpenAIInvoker invoker;

    @BeforeEach
    void setUp() {
        invoker = new OpenAIInvoker(
                "test-key", "https://api.openai.com", "gpt-4",
                0.7, 2048,
                RestClient.builder(), WebClient.builder()
        );
    }

    @Test
    void parseStreamChunkWithUsage() {
        String json = "{"
                + "\"choices\":[{"
                + "\"delta\":{\"content\":\"Hello\"},"
                + "\"finish_reason\":\"stop\""
                + "}],"
                + "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":20,\"total_tokens\":30}"
                + "}";

        ChatChunk chunk = invoker.parseStreamChunk(json);

        assertNotNull(chunk);
        assertEquals("Hello", chunk.getDelta());
        assertEquals("stop", chunk.getFinishReason());
        assertNotNull(chunk.getUsage());
        assertEquals(10, chunk.getUsage().getPromptTokens());
        assertEquals(20, chunk.getUsage().getCompletionTokens());
        assertEquals(30, chunk.getUsage().getTotalTokens());
    }

    @Test
    void parseStreamChunkWithoutUsage() {
        String json = "{"
                + "\"choices\":[{"
                + "\"delta\":{\"content\":\"Hi\"},"
                + "\"finish_reason\":null"
                + "}]"
                + "}";

        ChatChunk chunk = invoker.parseStreamChunk(json);

        assertNotNull(chunk);
        assertEquals("Hi", chunk.getDelta());
        assertNull(chunk.getUsage());
    }

    @Test
    void parseStreamChunkWithNullUsage() {
        String json = "{"
                + "\"choices\":[{"
                + "\"delta\":{\"content\":\"test\"}"
                + "}],"
                + "\"usage\":null"
                + "}";

        ChatChunk chunk = invoker.parseStreamChunk(json);

        assertNotNull(chunk);
        assertEquals("test", chunk.getDelta());
        assertNotNull(chunk.getUsage());
        assertNull(chunk.getUsage().getPromptTokens());
        assertNull(chunk.getUsage().getCompletionTokens());
        assertNull(chunk.getUsage().getTotalTokens());
    }

    @Test
    void parseStreamChunkWithPartialUsage() {
        String json = "{"
                + "\"choices\":[{"
                + "\"delta\":{\"content\":\"partial\"}"
                + "}],"
                + "\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":null,\"total_tokens\":null}"
                + "}";

        ChatChunk chunk = invoker.parseStreamChunk(json);

        assertNotNull(chunk);
        assertEquals("partial", chunk.getDelta());
        assertNotNull(chunk.getUsage());
        assertEquals(5, chunk.getUsage().getPromptTokens());
        assertEquals(0, chunk.getUsage().getCompletionTokens());
        assertEquals(0, chunk.getUsage().getTotalTokens());
    }

    @Test
    void parseStreamChunkMalformedJsonReturnsEmptyChunk() {
        ChatChunk chunk = invoker.parseStreamChunk("{invalid json");

        assertNotNull(chunk);
        assertNull(chunk.getDelta());
        assertNull(chunk.getUsage());
    }

    @Test
    void parseStreamChunkEmptyChoices() {
        String json = "{\"choices\":[],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":2,\"total_tokens\":3}}";

        ChatChunk chunk = invoker.parseStreamChunk(json);

        assertNotNull(chunk);
        assertNull(chunk.getUsage());
    }
}
