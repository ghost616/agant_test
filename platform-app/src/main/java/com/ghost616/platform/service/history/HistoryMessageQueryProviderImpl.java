package com.ghost616.platform.service.history;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.agentinteg.history.HistoryMessageItem;
import com.ghost616.agentinteg.history.HistoryMessageQueryProvider;
import com.ghost616.platform.entity.Message;
import com.ghost616.platform.entity.MessageToolCall;
import com.ghost616.platform.repository.MessageMapper;
import com.ghost616.platform.repository.MessageToolCallMapper;
import com.ghost616.platform.util.IdConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 历史消息查询 Provider 实现，基于 platform-app 的持久化组件提供历史消息的查询能力。
 */
@Component
@RequiredArgsConstructor
public class HistoryMessageQueryProviderImpl implements HistoryMessageQueryProvider {

    private final MessageMapper messageMapper;
    private final MessageToolCallMapper messageToolCallMapper;

    @Override
    public List<HistoryMessageItem> getMessagesBySeqs(String sessionId, List<Integer> seqs, boolean includeReasoning) {
        Long sid = IdConverter.parse(sessionId);
        if (sid == null || seqs == null || seqs.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, sid)
                .eq(Message::getRollback, false)
                .in(Message::getSequenceNum, seqs)
                .orderByAsc(Message::getSequenceNum);
        List<Message> messages = messageMapper.selectList(wrapper);
        List<HistoryMessageItem> result = new ArrayList<>(messages.size());
        for (Message message : messages) {
            result.add(buildItem(message, includeReasoning));
        }
        return result;
    }

    private HistoryMessageItem buildItem(Message message, boolean includeReasoning) {
        String reasoning = includeReasoning ? message.getReasoning() : null;
        List<MessageToolCall> toolCallRecords = messageToolCallMapper.selectList(
                new LambdaQueryWrapper<MessageToolCall>()
                        .eq(MessageToolCall::getMessageId, message.getId()));
        if ("tool".equals(message.getRole())) {
            return new HistoryMessageItem(message.getRole(), message.getContent(), reasoning,
                    null, buildToolResult(message, toolCallRecords));
        }
        List<HistoryMessageItem.HistoryToolCallItem> callItems = new ArrayList<>();
        for (MessageToolCall record : toolCallRecords) {
            String type = record.getType() != null ? record.getType() : "function";
            if ("function".equals(type)) {
                callItems.add(new HistoryMessageItem.HistoryToolCallItem(record.getToolCallId(),
                        record.getToolCallName(), record.getToolCallArguments()));
            }
        }
        return new HistoryMessageItem(message.getRole(), message.getContent(), reasoning,
                callItems.isEmpty() ? null : callItems, null);
    }

    private HistoryMessageItem.HistoryToolResultItem buildToolResult(Message message,
                                                                     List<MessageToolCall> toolCallRecords) {
        String toolCallId = message.getToolCallId();
        if (toolCallId != null) {
            for (MessageToolCall record : toolCallRecords) {
                if (toolCallId.equals(record.getToolCallId())) {
                    return new HistoryMessageItem.HistoryToolResultItem(record.getToolCallId(),
                            record.getToolCallName());
                }
            }
        }
        for (MessageToolCall record : toolCallRecords) {
            if ("tool_result".equals(record.getType()) && record.getToolCallId() != null) {
                return new HistoryMessageItem.HistoryToolResultItem(record.getToolCallId(),
                        record.getToolCallName());
            }
        }
        return null;
    }
}
