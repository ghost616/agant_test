package com.ghost616.platform.service.history;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghost616.agentinteg.history.HistoryMessageItem;
import com.ghost616.platform.entity.Message;
import com.ghost616.platform.entity.MessageToolCall;
import com.ghost616.platform.repository.MessageMapper;
import com.ghost616.platform.repository.MessageToolCallMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryMessageQueryProviderImplTest {

    @Mock
    private MessageMapper messageMapper;
    @Mock
    private MessageToolCallMapper messageToolCallMapper;

    private HistoryMessageQueryProviderImpl provider;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Message.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), MessageToolCall.class);
        provider = new HistoryMessageQueryProviderImpl(messageMapper, messageToolCallMapper);
    }

    private Message message(Long id, String role, String content, String reasoning, Integer seq,
                            String toolCallId) {
        Message m = new Message();
        m.setId(id);
        m.setRole(role);
        m.setContent(content);
        m.setReasoning(reasoning);
        m.setSequenceNum(seq);
        m.setToolCallId(toolCallId);
        m.setRollback(false);
        return m;
    }

    private MessageToolCall call(Long id, Long messageId, String callId, String name, String args, String type) {
        MessageToolCall c = new MessageToolCall();
        c.setId(id);
        c.setMessageId(messageId);
        c.setToolCallId(callId);
        c.setToolCallName(name);
        c.setToolCallArguments(args);
        c.setType(type);
        return c;
    }

    @Test
    @DisplayName("getMessagesBySeqs: sessionId 为 null/空白或 seqs 为空时返回空列表")
    void getMessagesBySeqs_invalidArgs() {
        assertTrue(provider.getMessagesBySeqs(null, List.of(1), false).isEmpty());
        assertTrue(provider.getMessagesBySeqs("  ", List.of(1), false).isEmpty());
        assertTrue(provider.getMessagesBySeqs("100", null, false).isEmpty());
        assertTrue(provider.getMessagesBySeqs("100", List.of(), false).isEmpty());
        verifyNoInteractions(messageMapper);
    }

    @Test
    @DisplayName("getMessagesBySeqs: 按 sessionId+in sequenceNum 查询，过滤 rollback=false，assistant 工具调用组装")
    void getMessagesBySeqs_assistantToolCalls() {
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(1L, "assistant", "调用工具", "推理过程", 1, null),
                message(2L, "user", "你好", null, 2, null)));
        when(messageToolCallMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(
                        call(10L, 1L, "call-1", "getWeather", "{\"loc\":\"Beijing\"}", "function"),
                        call(11L, 1L, "call-2", "getTime", "{}", "function")))
                .thenReturn(List.of());

        List<HistoryMessageItem> result = provider.getMessagesBySeqs("100", List.of(1, 2), true);

        assertEquals(2, result.size());

        HistoryMessageItem assistant = result.get(0);
        assertEquals("assistant", assistant.role());
        assertEquals("调用工具", assistant.content());
        assertEquals("推理过程", assistant.reasoning());
        assertNotNull(assistant.toolCalls());
        assertEquals(2, assistant.toolCalls().size());
        assertEquals("call-1", assistant.toolCalls().get(0).toolCallId());
        assertEquals("getWeather", assistant.toolCalls().get(0).toolCallName());
        assertEquals("{\"loc\":\"Beijing\"}", assistant.toolCalls().get(0).toolCallArguments());
        assertNull(assistant.toolResult());

        HistoryMessageItem user = result.get(1);
        assertEquals("user", user.role());
        assertEquals("你好", user.content());
        assertNull(user.toolCalls());
        assertNull(user.toolResult());

        verify(messageMapper).selectList(argThat(wrapper -> {
            String sql = wrapper.getSqlSegment();
            return sql.contains("rollback") && sql.contains("sequence_num") && sql.contains("IN");
        }));
        verify(messageToolCallMapper, times(2)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("getMessagesBySeqs: includeReasoning=false 时 reasoning 为 null")
    void getMessagesBySeqs_hideReasoning() {
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(1L, "assistant", "回复", "隐藏的推理", 1, null)));
        when(messageToolCallMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<HistoryMessageItem> result = provider.getMessagesBySeqs("100", List.of(1), false);

        assertEquals(1, result.size());
        assertNull(result.get(0).reasoning());
    }

    @Test
    @DisplayName("getMessagesBySeqs: tool 消息按 message.toolCallId 匹配组装 toolResult")
    void getMessagesBySeqs_toolResultByToolCallId() {
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(1L, "tool", "{\"temp\":25}", null, 3, "call-1")));
        when(messageToolCallMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                call(10L, 1L, "call-1", "getWeather", "{\"loc\":\"Beijing\"}", "function")));

        List<HistoryMessageItem> result = provider.getMessagesBySeqs("100", List.of(3), true);

        assertEquals(1, result.size());
        HistoryMessageItem tool = result.get(0);
        assertEquals("tool", tool.role());
        assertEquals("{\"temp\":25}", tool.content());
        assertNull(tool.toolCalls());
        assertNotNull(tool.toolResult());
        assertEquals("call-1", tool.toolResult().toolCallId());
        assertEquals("getWeather", tool.toolResult().toolCallName());
    }

    @Test
    @DisplayName("getMessagesBySeqs: tool 消息按 type=tool_result 记录组装 toolResult")
    void getMessagesBySeqs_toolResultByType() {
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(1L, "tool", "{\"temp\":25}", null, 3, null)));
        when(messageToolCallMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                call(10L, 1L, "call-9", "getWeather", null, "tool_result")));

        List<HistoryMessageItem> result = provider.getMessagesBySeqs("100", List.of(3), true);

        assertEquals(1, result.size());
        HistoryMessageItem tool = result.get(0);
        assertNotNull(tool.toolResult());
        assertEquals("call-9", tool.toolResult().toolCallId());
        assertEquals("getWeather", tool.toolResult().toolCallName());
    }

    @Test
    @DisplayName("getMessagesBySeqs: tool 消息无匹配记录时 toolResult 为 null")
    void getMessagesBySeqs_toolResultNullWhenNoMatch() {
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(1L, "tool", "{\"temp\":25}", null, 3, "call-unknown")));
        when(messageToolCallMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                call(10L, 1L, "call-1", "getWeather", null, "function")));

        List<HistoryMessageItem> result = provider.getMessagesBySeqs("100", List.of(3), true);

        assertEquals(1, result.size());
        assertNull(result.get(0).toolResult());
    }

    @Test
    @DisplayName("getMessagesBySeqs: assistant 消息仅 type=function 的工具调用被组装")
    void getMessagesBySeqs_filterNonFunctionCalls() {
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(1L, "assistant", "调用工具", null, 1, null)));
        when(messageToolCallMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                call(10L, 1L, "call-1", "getWeather", "{}", "function"),
                call(11L, 1L, "call-2", "web", null, "web_search")));

        List<HistoryMessageItem> result = provider.getMessagesBySeqs("100", List.of(1), true);

        assertEquals(1, result.size());
        assertNotNull(result.get(0).toolCalls());
        assertEquals(1, result.get(0).toolCalls().size());
        assertEquals("call-1", result.get(0).toolCalls().get(0).toolCallId());
    }

    @Test
    @DisplayName("getMessagesBySeqs: 无匹配消息时返回空列表")
    void getMessagesBySeqs_noMessages() {
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<HistoryMessageItem> result = provider.getMessagesBySeqs("100", List.of(99), false);

        assertTrue(result.isEmpty());
        verify(messageToolCallMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }
}
