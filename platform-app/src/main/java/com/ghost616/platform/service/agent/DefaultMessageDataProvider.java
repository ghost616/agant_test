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
import com.ghost616.agentbase.service.agent.MessageDataProvider.WebSearchCallData;
import com.ghost616.agentbase.service.agent.MessageDataProvider.CustomToolCallData;
import com.ghost616.agentbase.util.JsonMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.util.IdConverter;
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
    private final MessageToolCallService messageToolCallService;
    private final SessionMapper sessionMapper;

    @Override
    public String saveMessage(String sessionId, String role, String content, String reasoning,
                               String toolCallId, String toolResult, List<ToolCallData> toolCalls,
                               UsageInfo usage, List<WebSearchCallData> webSearchCall, List<CustomToolCallData> customToolCall) {
        Long sid = IdConverter.parse(sessionId);
        Message message = new Message();
        message.setSessionId(sid);
        message.setRole(role);
        message.setContent(content);
        message.setReasoning(reasoning);
        message.setToolCallId(toolCallId);
        message.setToolResult(toolResult);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, sid)
                .eq(Message::getRollback, false)
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
                sessionMapper.addTotalTokenUsed(sid, addTokens);
            }
        }

        messageMapper.insert(message);
        Long messageId = message.getId();

        List<MessageToolCall> batchToolCalls = new ArrayList<>();
        if (toolCalls != null) {
            for (ToolCallData tc : toolCalls) {
                MessageToolCall mtc = new MessageToolCall();
                mtc.setMessageId(messageId);
                mtc.setToolCallId(tc.toolCallId());
                mtc.setToolCallName(tc.toolCallName());
                mtc.setToolCallArguments(tc.toolCallArguments());
                mtc.setType(tc.type());
                batchToolCalls.add(mtc);
            }
        }
        if (webSearchCall != null) {
            for (WebSearchCallData data : webSearchCall) {
                String json = toJson(data);
                if (json != null) {
                    MessageToolCall mtc = new MessageToolCall();
                    mtc.setMessageId(messageId);
                    mtc.setType("web_search_call");
                    mtc.setWebSearchCall(json);
                    batchToolCalls.add(mtc);
                }
            }
        }
        if (customToolCall != null) {
            for (CustomToolCallData data : customToolCall) {
                String json = toJson(data);
                if (json != null) {
                    MessageToolCall mtc = new MessageToolCall();
                    mtc.setMessageId(messageId);
                    mtc.setType("custom_tool_call");
                    mtc.setCustomToolCall(json);
                    batchToolCalls.add(mtc);
                }
            }
        }
        if (!batchToolCalls.isEmpty()) {
            messageToolCallService.saveBatch(batchToolCalls);
        }

        return IdConverter.toString(messageId);
    }

    @Override
    public List<MessageDTO> getMessages(String sessionId) {
        Long sid = IdConverter.parse(sessionId);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, sid)
                .eq(Message::getRollback, false)
                .orderByAsc(Message::getSequenceNum);
        List<Message> messages = messageMapper.selectList(wrapper);

        List<MessageDTO> result = new ArrayList<>();
        for (Message msg : messages) {
            LambdaQueryWrapper<MessageToolCall> tcWrapper = new LambdaQueryWrapper<>();
            tcWrapper.eq(MessageToolCall::getMessageId, msg.getId());
            List<MessageToolCall> toolCalls = messageToolCallMapper.selectList(tcWrapper);

            List<ToolCallData> toolCallDataList = new ArrayList<>();
            List<WebSearchCallData> webSearchCallDataList = null;
            List<CustomToolCallData> customToolCallDataList = null;
            for (MessageToolCall tc : toolCalls) {
                String type = tc.getType() != null ? tc.getType() : "function";
                String webJson = tc.getWebSearchCall();
                String customJson = tc.getCustomToolCall();
                if ("web_search_call".equals(type) && webJson != null && !webJson.isEmpty()) {
                    WebSearchCallData data = deserializeWebSearchCall(webJson);
                    if (data != null) {
                        if (webSearchCallDataList == null) {
                            webSearchCallDataList = new ArrayList<>();
                        }
                        webSearchCallDataList.add(data);
                    }
                } else if ("custom_tool_call".equals(type) && customJson != null && !customJson.isEmpty()) {
                    CustomToolCallData data = deserializeCustomToolCall(customJson);
                    if (data != null) {
                        if (customToolCallDataList == null) {
                            customToolCallDataList = new ArrayList<>();
                        }
                        customToolCallDataList.add(data);
                    }
                } else {
                    toolCallDataList.add(new ToolCallData(tc.getToolCallId(), tc.getToolCallName(),
                            tc.getToolCallArguments(), type));
                }
            }

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
                    IdConverter.toString(msg.getId()), IdConverter.toString(msg.getSessionId()), msg.getRole(), msg.getContent(),
                    msg.getReasoning(), msg.getToolCallId(), msg.getSequenceNum(),
                    msg.getCreateTime(), msg.getToolResult(), toolCallDataList, usageInfo,
                    msg.getRollback(), webSearchCallDataList, customToolCallDataList));
        }

        return result;
    }

    @Override
    public int rollbackToLastUserMessage(String sessionId) {
        Long sid = IdConverter.parse(sessionId);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, sid)
                .eq(Message::getRollback, false)
                .eq(Message::getRole, "user")
                .orderByDesc(Message::getSequenceNum)
                .last("LIMIT 1");
        Message lastUserMessage = messageMapper.selectOne(wrapper);
        if (lastUserMessage == null) {
            throw new BusinessException(ErrorCode.SESSION_NO_USER_MESSAGE);
        }

        Integer sequenceNum = lastUserMessage.getSequenceNum();

        LambdaQueryWrapper<Message> idWrapper = new LambdaQueryWrapper<>();
        idWrapper.eq(Message::getSessionId, sid)
                .eq(Message::getRollback, false)
                .ge(Message::getSequenceNum, sequenceNum);
        List<Message> messagesToDelete = messageMapper.selectList(idWrapper);
        List<Long> messageIds = messagesToDelete.stream()
                .map(Message::getId)
                .collect(Collectors.toList());

        if (!messageIds.isEmpty()) {
            messageToolCallMapper.deleteByMessageIds(messageIds);
        }

        return messageMapper.rollbackBySessionIdAndGeSequenceNum(sid, sequenceNum);
    }

    private String toJson(Object obj) {
        try {
            return JsonMapper.MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("序列化 webSearchCall/customToolCall 失败", e);
            return null;
        }
    }

    private WebSearchCallData deserializeWebSearchCall(String json) {
        try {
            return JsonMapper.MAPPER.readValue(json, WebSearchCallData.class);
        } catch (Exception e) {
            log.warn("反序列化 webSearchCall 失败", e);
            return null;
        }
    }

    private CustomToolCallData deserializeCustomToolCall(String json) {
        try {
            return JsonMapper.MAPPER.readValue(json, CustomToolCallData.class);
        } catch (Exception e) {
            log.warn("反序列化 customToolCall 失败", e);
            return null;
        }
    }
}