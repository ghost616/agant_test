package com.ghost616.platform.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.agentbase.dto.model.UsageInfo;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.agent.MessageDataProvider.MessageDTO;
import com.ghost616.agentbase.service.agent.MessageDataProvider.ToolCallData;
import com.ghost616.platform.entity.Message;
import com.ghost616.platform.entity.MessageToolCall;
import com.ghost616.platform.repository.MessageMapper;
import com.ghost616.platform.repository.MessageToolCallMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultMessageDataProviderTest {

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private MessageToolCallMapper messageToolCallMapper;

    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    @Captor
    private ArgumentCaptor<MessageToolCall> toolCallCaptor;

    private DefaultMessageDataProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DefaultMessageDataProvider(messageMapper, messageToolCallMapper);
    }

    @Test
    void saveMessage_无toolCalls_保存消息并返回ID() {
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(messageMapper.insert(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return 1;
        });

        UsageInfo usage = UsageInfo.builder().promptTokens(10).completionTokens(20).totalTokens(30).build();
        Long result = provider.saveMessage(1L, "user", "hello", null, null, null, null, usage);

        assertEquals(100L, result);
        verify(messageMapper).insert(messageCaptor.capture());
        Message saved = messageCaptor.getValue();
        assertEquals(1L, saved.getSessionId());
        assertEquals("user", saved.getRole());
        assertEquals("hello", saved.getContent());
        assertEquals(1, saved.getSequenceNum());
        verify(messageToolCallMapper, never()).insert(any(MessageToolCall.class));
    }

    @Test
    void saveMessage_有toolCalls_保存消息和工具调用() {
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(messageMapper.insert(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(200L);
            return 1;
        });

        List<ToolCallData> toolCalls = Arrays.asList(
                new ToolCallData("tc1", "func1", "{}"),
                new ToolCallData("tc2", "func2", "{\"key\":\"val\"}")
        );

        provider.saveMessage(1L, "assistant", "response", "thinking...", "call-1", null, toolCalls, null);

        verify(messageMapper).insert(messageCaptor.capture());
        assertEquals("assistant", messageCaptor.getValue().getRole());
        assertEquals("response", messageCaptor.getValue().getContent());
        assertEquals("thinking...", messageCaptor.getValue().getReasoning());
        assertEquals("call-1", messageCaptor.getValue().getToolCallId());

        verify(messageToolCallMapper, times(2)).insert(toolCallCaptor.capture());
        List<MessageToolCall> capturedToolCalls = toolCallCaptor.getAllValues();
        assertEquals("tc1", capturedToolCalls.get(0).getToolCallId());
        assertEquals("func1", capturedToolCalls.get(0).getToolCallName());
        assertEquals("{}", capturedToolCalls.get(0).getToolCallArguments());
        assertEquals("tc2", capturedToolCalls.get(1).getToolCallId());
        assertEquals("func2", capturedToolCalls.get(1).getToolCallName());
    }

    @Test
    void saveMessage_已有消息_序列号递增() {
        Message lastMsg = new Message();
        lastMsg.setSequenceNum(5);
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(lastMsg);
        when(messageMapper.insert(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(300L);
            return 1;
        });

        provider.saveMessage(1L, "user", "next", null, null, null, null, null);

        verify(messageMapper).insert(messageCaptor.capture());
        assertEquals(6, messageCaptor.getValue().getSequenceNum());
    }

    @Test
    void saveMessage_toolCalls为null_不插入工具调用() {
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(messageMapper.insert(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(400L);
            return 1;
        });

        provider.saveMessage(1L, "user", "test", null, null, null, null, null);

        verify(messageMapper).insert(any(Message.class));
        verify(messageToolCallMapper, never()).insert(any(MessageToolCall.class));
    }

    @Test
    void getMessages_有toolCalls_返回MessageDTO且usage为null() {
        Message msg = new Message();
        msg.setId(1L);
        msg.setSessionId(10L);
        msg.setRole("assistant");
        msg.setContent("response");
        msg.setReasoning("reason");
        msg.setToolCallId("tc-id");
        msg.setSequenceNum(1);
        msg.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        msg.setToolResult("result");
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(msg));

        MessageToolCall mtc = new MessageToolCall();
        mtc.setToolCallId("call-1");
        mtc.setToolCallName("func1");
        mtc.setToolCallArguments("{}");
        when(messageToolCallMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(mtc));

        List<MessageDTO> result = provider.getMessages(10L);

        assertEquals(1, result.size());
        MessageDTO dto = result.get(0);
        assertEquals(1L, dto.id());
        assertEquals(10L, dto.sessionId());
        assertEquals("assistant", dto.role());
        assertEquals("response", dto.content());
        assertEquals("reason", dto.reasoning());
        assertEquals("tc-id", dto.toolCallId());
        assertEquals(1, dto.sequenceNum());
        assertEquals(msg.getCreateTime(), dto.createTime());
        assertEquals("result", dto.toolResult());
        assertNull(dto.usage());
        assertEquals(1, dto.toolCalls().size());
        assertEquals("call-1", dto.toolCalls().get(0).toolCallId());
        assertEquals("func1", dto.toolCalls().get(0).toolCallName());
    }

    @Test
    void getMessages_无toolCalls_toolCalls为空列表usage为null() {
        Message msg = new Message();
        msg.setId(2L);
        msg.setSessionId(10L);
        msg.setRole("user");
        msg.setContent("hi");
        msg.setSequenceNum(1);
        msg.setCreateTime(LocalDateTime.now());
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(msg));
        when(messageToolCallMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<MessageDTO> result = provider.getMessages(10L);

        assertEquals(1, result.size());
        assertTrue(result.get(0).toolCalls().isEmpty());
        assertNull(result.get(0).usage());
    }

    @Test
    void getMessages_无消息_返回空列表() {
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<MessageDTO> result = provider.getMessages(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void rollbackToLastUserMessage_存在用户消息_删除后续消息() {
        Message userMsg = new Message();
        userMsg.setId(5L);
        userMsg.setSequenceNum(3);
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userMsg);

        Message msg1 = new Message();
        msg1.setId(5L);
        Message msg2 = new Message();
        msg2.setId(6L);
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(msg1, msg2));
        when(messageToolCallMapper.deleteByMessageIds(anyList())).thenReturn(2);
        when(messageMapper.deleteBySessionIdAndGeSequenceNum(10L, 3)).thenReturn(2);

        int deleted = provider.rollbackToLastUserMessage(10L);

        assertEquals(2, deleted);
        verify(messageToolCallMapper).deleteByMessageIds(Arrays.asList(5L, 6L));
        verify(messageMapper).deleteBySessionIdAndGeSequenceNum(10L, 3);
    }

    @Test
    void rollbackToLastUserMessage_无用户消息_抛出BusinessException() {
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> provider.rollbackToLastUserMessage(1L));
        verify(messageMapper, never()).deleteBySessionIdAndGeSequenceNum(any(), any());
    }
}
