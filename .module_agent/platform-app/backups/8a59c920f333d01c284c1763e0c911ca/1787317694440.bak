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
import org.mockito.ArgumentCaptor;
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

    private static final String SESSION_ID = "1";

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
                Message.class);
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
        when(subSessionWebSocketModeResolver.isWebSocketSubSession(SESSION_ID)).thenReturn(false);

        assertFalse(provider.shouldSendResultToParent(SESSION_ID));

        verify(messageMapper, never()).selectOne(any(LambdaQueryWrapper.class));
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
        when(subSessionWebSocketModeResolver.isWebSocketSubSession(SESSION_ID)).thenReturn(true);
        // 第一步：最后一条 user 消息查询无结果（数据库无 role=user 消息）
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertFalse(provider.shouldSendResultToParent(SESSION_ID));

        verify(messageMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(messageToolCallMapper, never()).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldSendResultToParent_最近user消息后无消息_返回false() {
        when(subSessionWebSocketModeResolver.isWebSocketSubSession(SESSION_ID)).thenReturn(true);
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(message(1L, "user", 1));
        // 第二步：索引之后的消息链为空（最近 user 消息已是最后一条）
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertFalse(provider.shouldSendResultToParent(SESSION_ID));

        verify(messageToolCallMapper, never()).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldSendResultToParent_消息链已调用send_result_to_parent_返回false() {
        when(subSessionWebSocketModeResolver.isWebSocketSubSession(SESSION_ID)).thenReturn(true);
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(message(1L, "user", 1));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(2L, "assistant", 2),
                message(3L, "tool", 3),
                message(4L, "assistant", 4)));
        when(messageToolCallMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertFalse(provider.shouldSendResultToParent(SESSION_ID));
    }

    @Test
    void shouldSendResultToParent_消息链未调用send_result_to_parent_返回true() {
        when(subSessionWebSocketModeResolver.isWebSocketSubSession(SESSION_ID)).thenReturn(true);
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(message(1L, "user", 1));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(2L, "assistant", 2)));
        when(messageToolCallMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertTrue(provider.shouldSendResultToParent(SESSION_ID));
    }

    @Test
    void shouldSendResultToParent_仅在最近user消息之前调用过_返回true() {
        when(subSessionWebSocketModeResolver.isWebSocketSubSession(SESSION_ID)).thenReturn(true);
        // 最近一条 user 消息 sequenceNum=3（此前 user(1)/assistant(2) 为历史消息，不参与链查询）
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(message(3L, "user", 3));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(4L, "assistant", 4)));
        when(messageToolCallMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertTrue(provider.shouldSendResultToParent(SESSION_ID));

        // 分步查询：消息链查询仅携带索引之后的消息 ID（4），历史消息 ID（1/2）不进入 send_result_to_parent 检查
        verify(messageToolCallMapper).selectCount(argThat((LambdaQueryWrapper<MessageToolCall> wrapper) -> {
            wrapper.getSqlSegment(); // 渲染 SQL，触发参数填充
            return wrapper.getParamNameValuePairs().values().stream()
                    .noneMatch(value -> value.equals(1L) || value.equals(2L));
        }));
    }

    @Test
    void shouldSendResultToParent_工具名查询按send_result_to_parent过滤() {
        when(subSessionWebSocketModeResolver.isWebSocketSubSession(SESSION_ID)).thenReturn(true);
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(message(1L, "user", 1));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(2L, "assistant", 2)));
        when(messageToolCallMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        provider.shouldSendResultToParent(SESSION_ID);

        verify(messageToolCallMapper).selectCount(argThat((LambdaQueryWrapper<MessageToolCall> wrapper) -> {
            String sql = wrapper.getSqlSegment();
            return sql.contains("message_id") && sql.contains("tool_call_name")
                    && wrapper.getParamNameValuePairs().containsValue(SendResultToParentTool.TOOL_NAME);
        }));
    }

    @Test
    void shouldSendResultToParent_最后user消息查询含role过滤与sequenceNum倒序() {
        when(subSessionWebSocketModeResolver.isWebSocketSubSession(SESSION_ID)).thenReturn(true);
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(message(1L, "user", 5));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(2L, "assistant", 6)));
        when(messageToolCallMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertTrue(provider.shouldSendResultToParent(SESSION_ID));

        ArgumentCaptor<LambdaQueryWrapper<Message>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(messageMapper).selectOne(captor.capture());
        LambdaQueryWrapper<Message> wrapper = captor.getValue();
        String sql = wrapper.getSqlSegment(); // 渲染 SQL，触发参数填充
        assertTrue(sql.contains("session_id"));
        assertTrue(sql.contains("role"));
        assertTrue(sql.contains("rollback"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("user"));
        String orderBy = wrapper.getExpression().getOrderBy().getSqlSegment();
        assertTrue(orderBy.contains("sequence_num"));
        assertTrue(orderBy.contains("DESC"));
    }

    @Test
    void shouldSendResultToParent_消息链查询按sequenceNum大于索引过滤() {
        when(subSessionWebSocketModeResolver.isWebSocketSubSession(SESSION_ID)).thenReturn(true);
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(message(1L, "user", 3));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(2L, "assistant", 4)));
        when(messageToolCallMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertTrue(provider.shouldSendResultToParent(SESSION_ID));

        ArgumentCaptor<LambdaQueryWrapper<Message>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(messageMapper).selectList(captor.capture());
        LambdaQueryWrapper<Message> wrapper = captor.getValue();
        String sql = wrapper.getSqlSegment(); // 渲染 SQL，触发参数填充
        assertTrue(sql.contains("session_id"));
        assertTrue(sql.contains("rollback"));
        assertTrue(sql.contains("sequence_num"));
        assertTrue(sql.contains(">"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(3));
        String orderBy = wrapper.getExpression().getOrderBy().getSqlSegment();
        assertTrue(orderBy.contains("sequence_num"));
        assertTrue(orderBy.contains("ASC"));
    }
}
