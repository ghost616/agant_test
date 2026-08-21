package com.ghost616.platform.service.agent;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghost616.agentinteg.tool.SendResultToParentTool;
import com.ghost616.platform.entity.Message;
import com.ghost616.platform.entity.MessageToolCall;
import com.ghost616.platform.repository.MessageMapper;
import com.ghost616.platform.repository.MessageToolCallMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubSessionResultProviderImplTest {

    @Mock
    private SubSessionWebSocketModeResolver subSessionWebSocketModeResolver;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private MessageToolCallMapper messageToolCallMapper;

    private SubSessionResultProviderImpl provider;

    @BeforeAll
    static void initTableInfo() {
        // 初始化 MyBatis-Plus TableInfo 缓存，使 LambdaQueryWrapper.getSqlSegment() 在纯单测环境可解析列名
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                MessageToolCall.class);
    }

    @BeforeEach
    void setUp() {
        provider = new SubSessionResultProviderImpl(
                subSessionWebSocketModeResolver, messageMapper, messageToolCallMapper);
    }

    private Message message(Long id, String role, int sequenceNum) {
        Message message = new Message();
        message.setId(id);
        message.setRole(role);
        message.setSequenceNum(sequenceNum);
        return message;
    }

    @Test
    void shouldSendResultToParent_非WEBSOCKET子会话_返回false() {
        when(subSessionWebSocketModeResolver.isWebSocketSubSession("1")).thenReturn(false);

        assertFalse(provider.shouldSendResultToParent("1"));

        verify(messageMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(messageToolCallMapper, never()).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldSendResultToParent_会话ID为null_返回false() {
        assertFalse(provider.shouldSendResultToParent(null));
    }

    @Test
    void shouldSendResultToParent_会话ID为空白_返回false() {
        assertFalse(provider.shouldSendResultToParent("  "));
    }

    @Test
    void shouldSendResultToParent_无user消息_返回false() {
        when(subSessionWebSocketModeResolver.isWebSocketSubSession("1")).thenReturn(true);
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(1L, "assistant", 1),
                message(2L, "tool", 2)));

        assertFalse(provider.shouldSendResultToParent("1"));

        verify(messageToolCallMapper, never()).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldSendResultToParent_最近user消息后无消息_返回false() {
        when(subSessionWebSocketModeResolver.isWebSocketSubSession("1")).thenReturn(true);
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(1L, "user", 1)));

        assertFalse(provider.shouldSendResultToParent("1"));

        verify(messageToolCallMapper, never()).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldSendResultToParent_消息链已调用send_result_to_parent_返回false() {
        when(subSessionWebSocketModeResolver.isWebSocketSubSession("1")).thenReturn(true);
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(1L, "user", 1),
                message(2L, "assistant", 2),
                message(3L, "tool", 3),
                message(4L, "assistant", 4)));
        when(messageToolCallMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertFalse(provider.shouldSendResultToParent("1"));
    }

    @Test
    void shouldSendResultToParent_消息链未调用send_result_to_parent_返回true() {
        when(subSessionWebSocketModeResolver.isWebSocketSubSession("1")).thenReturn(true);
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(1L, "user", 1),
                message(2L, "assistant", 2)));
        when(messageToolCallMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertTrue(provider.shouldSendResultToParent("1"));
    }

    @Test
    void shouldSendResultToParent_仅在最近user消息之前调用过_返回true() {
        when(subSessionWebSocketModeResolver.isWebSocketSubSession("1")).thenReturn(true);
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(1L, "user", 1),
                message(2L, "assistant", 2),
                message(3L, "user", 3),
                message(4L, "assistant", 4)));
        when(messageToolCallMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertTrue(provider.shouldSendResultToParent("1"));
    }

    @Test
    void shouldSendResultToParent_工具名查询按send_result_to_parent过滤() {
        when(subSessionWebSocketModeResolver.isWebSocketSubSession("1")).thenReturn(true);
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(1L, "user", 1),
                message(2L, "assistant", 2)));
        when(messageToolCallMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        provider.shouldSendResultToParent("1");

        verify(messageToolCallMapper).selectCount(argThat((LambdaQueryWrapper<MessageToolCall> wrapper) -> {
            String sql = wrapper.getSqlSegment();
            return sql.contains("message_id") && sql.contains("tool_call_name")
                    && wrapper.getParamNameValuePairs().containsValue(SendResultToParentTool.TOOL_NAME);
        }));
    }
}