package com.ghost616.agentinteg.model.invoker;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.dto.model.ToolInfo;
import com.ghost616.agentbase.dto.model.UsageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

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

    @Test
    void buildMessagesToolRoleMapsToolCallIdAndName() {
        Message toolMsg = Message.builder()
                .role("tool")
                .content("result")
                .toolInfo(new ToolInfo("call_1", "getWeather"))
                .build();

        List<Map<String, Object>> result = invoker.buildMessages(List.of(toolMsg));

        assertEquals(1, result.size());
        Map<String, Object> m = result.get(0);
        assertEquals("tool", m.get("role"));
        assertEquals("result", m.get("content"));
        assertEquals("call_1", m.get("tool_call_id"));
        assertEquals("getWeather", m.get("name"));
    }

    @Test
    void buildMessagesToolRoleWithNullToolNameOmitsName() {
        Message toolMsg = Message.builder()
                .role("tool")
                .content("result")
                .toolInfo(new ToolInfo("call_2", null))
                .build();

        List<Map<String, Object>> result = invoker.buildMessages(List.of(toolMsg));

        assertEquals(1, result.size());
        Map<String, Object> m = result.get(0);
        assertEquals("tool", m.get("role"));
        assertEquals("call_2", m.get("tool_call_id"));
        assertFalse(m.containsKey("name"));
    }

    @Test
    void buildMessagesToolRoleWithNullToolInfoOmitsBoth() {
        Message toolMsg = Message.builder()
                .role("tool")
                .content("result")
                .build();

        List<Map<String, Object>> result = invoker.buildMessages(List.of(toolMsg));

        assertEquals(1, result.size());
        Map<String, Object> m = result.get(0);
        assertEquals("tool", m.get("role"));
        assertFalse(m.containsKey("tool_call_id"));
        assertFalse(m.containsKey("name"));
    }

    @Test
    void buildMessagesNonToolRoleHasNoNameField() {
        Message userMsg = Message.builder()
                .role("user")
                .content("hello")
                .build();

        List<Map<String, Object>> result = invoker.buildMessages(List.of(userMsg));

        assertEquals(1, result.size());
        Map<String, Object> m = result.get(0);
        assertEquals("user", m.get("role"));
        assertFalse(m.containsKey("name"));
        assertFalse(m.containsKey("tool_call_id"));
    }
}
