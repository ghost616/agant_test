package com.ghost616.platform.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.agentinteg.subsession.SubSessionResultProvider;
import com.ghost616.agentinteg.tool.SendResultToParentTool;
import com.ghost616.platform.entity.Message;
import com.ghost616.platform.entity.MessageToolCall;
import com.ghost616.platform.repository.MessageMapper;
import com.ghost616.platform.repository.MessageToolCallMapper;
import com.ghost616.platform.util.IdConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 子会话结果兜底回传 Provider 实现（@Component），实现 agent-integration 的 {@link SubSessionResultProvider} 接口。
 *
 * <p>判断指定子会话是否需要向父会话兜底回传执行结果：</p>
 * <ol>
 *   <li>复用 {@link SubSessionWebSocketModeResolver} 缓存判断当前会话是否为 WEBSOCKET 打开方式的子会话，
 *       非 WEBSOCKET 子会话直接返回无需发送；</li>
 *   <li>从数据库（MessageMapper）查询子会话全部有效消息，定位最近一条 role=user 的消息，
 *       取其之后的消息链（含 assistant/tool）；</li>
 *   <li>通过 MessageToolCallMapper 检查消息链上是否已调用 send_result_to_parent 工具
 *       （assistant 消息的 function tool_calls 与 tool 消息的 tool_result 记录均会落库为 toolCallName 匹配行）；</li>
 *   <li>已调用则无需发送（结果已回传），未调用则返回需要向父会话兜底发送。</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class SubSessionResultProviderImpl implements SubSessionResultProvider {

    private static final String ROLE_USER = "user";

    private final SubSessionWebSocketModeResolver subSessionWebSocketModeResolver;
    private final MessageMapper messageMapper;
    private final MessageToolCallMapper messageToolCallMapper;

    @Override
    public boolean shouldSendResultToParent(String sessionId) {
        // a. 非 WEBSOCKET 子会话无需兜底回传（复用解析器缓存判断）
        if (!subSessionWebSocketModeResolver.isWebSocketSubSession(sessionId)) {
            return false;
        }
        Long sid = IdConverter.parse(sessionId);
        if (sid == null) {
            return false;
        }
        // b. 查询消息，定位最近一条 user 消息之后的消息链
        List<Message> messages = queryMessages(sid);
        int lastUserIndex = findLastUserMessageIndex(messages);
        if (lastUserIndex < 0 || lastUserIndex >= messages.size() - 1) {
            return false;
        }
        List<Message> chain = messages.subList(lastUserIndex + 1, messages.size());
        // c/d. 链上已调用 send_result_to_parent 则无需发送，否则需要兜底回传
        return !hasCalledSendResultToParent(chain);
    }

    /**
     * 查询指定会话的全部有效消息（过滤已回滚消息），按 sequenceNum 升序返回。
     *
     * @param sessionId 会话 ID
     * @return 消息实体列表
     */
    private List<Message> queryMessages(Long sessionId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, sessionId)
                .eq(Message::getRollback, false)
                .orderByAsc(Message::getSequenceNum);
        return messageMapper.selectList(wrapper);
    }

    /**
     * 定位消息列表中最近一条 role=user 消息的下标。
     *
     * @param messages 消息列表（按 sequenceNum 升序）
     * @return 最近一条 user 消息的下标；无 user 消息时返回 -1
     */
    private int findLastUserMessageIndex(List<Message> messages) {
        int lastIndex = -1;
        for (int i = 0; i < messages.size(); i++) {
            if (ROLE_USER.equals(messages.get(i).getRole())) {
                lastIndex = i;
            }
        }
        return lastIndex;
    }

    /**
     * 检查消息链上是否已调用 send_result_to_parent 工具。
     * assistant 消息的 function tool_calls 与 tool 消息的 tool_result 记录
     * 均会落库为 MessageToolCall（toolCallName 为工具名），按消息 ID 集合 + 工具名匹配即可覆盖两种形态。
     *
     * @param chain 最近一条 user 消息之后的消息链
     * @return true 表示已调用过 send_result_to_parent
     */
    private boolean hasCalledSendResultToParent(List<Message> chain) {
        List<Long> messageIds = chain.stream()
                .map(Message::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (messageIds.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<MessageToolCall> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MessageToolCall::getMessageId, messageIds)
                .eq(MessageToolCall::getToolCallName, SendResultToParentTool.TOOL_NAME);
        Long count = messageToolCallMapper.selectCount(wrapper);
        return count != null && count > 0;
    }
}