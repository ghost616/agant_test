package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.dto.model.ToolInfo;
import com.ghost616.agentbase.dto.model.UsageInfo;
import com.ghost616.agentbase.enums.LogType;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageSaveLogDataTest {

    @Test
    void logType应返回MESSAGE_SAVE() {
        MessageSaveLogData data = MessageSaveLogData.builder().build();
        assertEquals(LogType.MESSAGE_SAVE, data.logType());
    }

    @Test
    void 字符串与列表字段应默认为null() {
        MessageSaveLogData data = MessageSaveLogData.builder().build();
        assertTrue(SessionLogData.class.isAssignableFrom(data.getClass()));
        assertNull(data.getSessionId());
        assertNull(data.getRole());
        assertNull(data.getMessageId());
        assertNull(data.getContent());
        assertNull(data.getReasoning());
        assertNull(data.getToolInfo());
        assertNull(data.getToolResult());
        assertNull(data.getToolCalls());
        assertNull(data.getUsage());
        assertNull(data.getWebSearchCall());
        assertNull(data.getCustomToolCall());
        assertNull(data.getConversationId());
    }

    @Test
    void builder应正确设置继承与自有字段() {
        var toolCalls = new ArrayList<>(List.of(
                new MessageDataProvider.ToolCallData("tc1", "getWeather", "{}")));
        var webSearchCalls = new ArrayList<>(List.of(
                new MessageDataProvider.WebSearchCallData("i1", 0, List.of())));
        var customToolCalls = new ArrayList<>(List.of(
                new MessageDataProvider.CustomToolCallData("i1", 0, "{}", "{}")));
        UsageInfo usage = new UsageInfo(1, 2, 3);
        MessageSaveLogData data = MessageSaveLogData.builder()
                .sessionId("s1")
                .role("assistant")
                .messageId("m1")
                .content("response")
                .reasoning("thinking")
                .toolInfo(new ToolInfo("tc1", "getWeather"))
                .toolResult("result_ok")
                .toolCalls(toolCalls)
                .usage(usage)
                .webSearchCall(webSearchCalls)
                .customToolCall(customToolCalls)
                .conversationId("conv-1")
                .build();

        assertEquals("s1", data.getSessionId());
        assertEquals("assistant", data.getRole());
        assertEquals("m1", data.getMessageId());
        assertEquals("response", data.getContent());
        assertEquals("thinking", data.getReasoning());
        assertEquals(new ToolInfo("tc1", "getWeather"), data.getToolInfo());
        assertEquals("result_ok", data.getToolResult());
        assertEquals(toolCalls, data.getToolCalls());
        assertSame(usage, data.getUsage());
        assertEquals(webSearchCalls, data.getWebSearchCall());
        assertEquals(customToolCalls, data.getCustomToolCall());
        assertEquals("conv-1", data.getConversationId());
    }
}
