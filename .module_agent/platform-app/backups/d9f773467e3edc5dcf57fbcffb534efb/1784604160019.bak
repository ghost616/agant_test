package com.ghost616.platform.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.platform.entity.Message;
import com.ghost616.platform.entity.MessageToolCall;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.platform.repository.MessageMapper;
import com.ghost616.platform.repository.MessageToolCallMapper;
import com.ghost616.agentbase.dto.model.UsageInfo;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.MessageDataProvider.ToolCallData;
import com.ghost616.agentbase.service.agent.MessageDataProvider.MessageDTO;
import com.ghost616.agentbase.util.JsonMapper;
import com.ghost616.platform.repository.SessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultMessageDataProvider implements MessageDataProvider {

    private final MessageMapper messageMapper;
    private final MessageToolCallMapper messageToolCallMapper;
    private final SessionMapper sessionMapper;

    @Override
    public Long saveMessage(Long sessionId, String role, String content, String reasoning,
                             String toolCallId, String toolResult, List<ToolCallData> toolCalls,
                             UsageInfo usage) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setReasoning(reasoning);
        message.setToolCallId(toolCallId);
        message.setToolResult(toolResult);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, sessionId)
                .orderByDesc(Message::getSequenceNum)
                .last("LIMIT 1");
        Message lastMessage = messageMapper.selectOne(wrapper);
        int sequenceNum = (lastMessage != null) ? lastMessage.getSequenceNum() + 1 : 1;
        message.setSequenceNum(sequenceNum);

        if (usage != null) {
            try {
                String jsonStr = JsonMapper.MAPPER.writeValueAsString(usage);
                message.setTokenUsage(jsonStr);
            } catch (Exception e) {
                log.warn("序列化 UsageInfo 失败", e);
            }
            long addTokens = 0;
            if (usage.getTotalTokens() != null) {
                addTokens = usage.getTotalTokens().longValue();
            } else {
                addTokens = (usage.getPromptTokens() != null ? usage.getPromptTokens().longValue() : 0L)
                          + (usage.getCompletionTokens() != null ? usage.getCompletionTokens().longValue() : 0L);
            }
            if (addTokens > 0) {
                sessionMapper.addTotalTokenUsed(sessionId, addTokens);
            }
        }

        messageMapper.insert(message);
        Long messageId = message.getId();

        if (toolCalls != null && !toolCalls.isEmpty()) {
            for (ToolCallData tc : toolCalls) {
                MessageToolCall mtc = new MessageToolCall();
                mtc.setMessageId(messageId);
                mtc.setToolCallId(tc.toolCallId());
                mtc.setToolCallName(tc.toolCallName());
                mtc.setToolCallArguments(tc.toolCallArguments());
                messageToolCallMapper.insert(mtc);
            }
        }

        return messageId;
    }

    @Override
    public List<MessageDTO> getMessages(Long sessionId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, sessionId)
                .orderByAsc(Message::getSequenceNum);
        List<Message> messages = messageMapper.selectList(wrapper);

        List<MessageDTO> result = new ArrayList<>();
        for (Message msg : messages) {
            LambdaQueryWrapper<MessageToolCall> tcWrapper = new LambdaQueryWrapper<>();
            tcWrapper.eq(MessageToolCall::getMessageId, msg.getId());
            List<MessageToolCall> toolCalls = messageToolCallMapper.selectList(tcWrapper);

            List<ToolCallData> toolCallDataList = toolCalls.stream()
                    .map(tc -> new ToolCallData(tc.getToolCallId(), tc.getToolCallName(), tc.getToolCallArguments()))
                    .collect(Collectors.toList());

            UsageInfo usageInfo = null;
            String tokenUsageStr = msg.getTokenUsage();
            if (tokenUsageStr != null && !tokenUsageStr.isEmpty()) {
                try {
                    usageInfo = JsonMapper.MAPPER.readValue(tokenUsageStr, UsageInfo.class);
                } catch (Exception e) {
                    log.warn("反序列化 tokenUsage 失败", e);
                }
            }
            result.add(new MessageDTO(
                    msg.getId(), msg.getSessionId(), msg.getRole(), msg.getContent(),
                    msg.getReasoning(), msg.getToolCallId(), msg.getSequenceNum(),
                    msg.getCreateTime(), msg.getToolResult(), toolCallDataList, usageInfo));
        }

        return result;
    }

    @Override
    public int rollbackToLastUserMessage(Long sessionId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, sessionId)
                .eq(Message::getRole, "user")
                .orderByDesc(Message::getSequenceNum)
                .last("LIMIT 1");
        Message lastUserMessage = messageMapper.selectOne(wrapper);
        if (lastUserMessage == null) {
            throw new BusinessException(ErrorCode.SESSION_NO_USER_MESSAGE);
        }

        Integer sequenceNum = lastUserMessage.getSequenceNum();

        LambdaQueryWrapper<Message> idWrapper = new LambdaQueryWrapper<>();
        idWrapper.eq(Message::getSessionId, sessionId)
                .ge(Message::getSequenceNum, sequenceNum);
        List<Message> messagesToDelete = messageMapper.selectList(idWrapper);
        List<Long> messageIds = messagesToDelete.stream()
                .map(Message::getId)
                .collect(Collectors.toList());

        long deductedTokens = 0;
        for (Message msg : messagesToDelete) {
            String tu = msg.getTokenUsage();
            if (tu != null && !tu.isEmpty()) {
                try {
                    UsageInfo u = JsonMapper.MAPPER.readValue(tu, UsageInfo.class);
                    if (u.getTotalTokens() != null) {
                        deductedTokens += u.getTotalTokens().longValue();
                    } else {
                        deductedTokens += (u.getPromptTokens() != null ? u.getPromptTokens().longValue() : 0L)
                                        + (u.getCompletionTokens() != null ? u.getCompletionTokens().longValue() : 0L);
                    }
                } catch (Exception e) {
                    log.warn("反序列化 tokenUsage 失败，跳过该消息扣减", e);
                }
            }
        }
        if (deductedTokens > 0) {
            sessionMapper.addTotalTokenUsed(sessionId, -deductedTokens);
        }

        if (!messageIds.isEmpty()) {
            messageToolCallMapper.deleteByMessageIds(messageIds);
        }

        return messageMapper.deleteBySessionIdAndGeSequenceNum(sessionId, sequenceNum);
    }
}