package com.ghost616.platform.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.agentbase.dto.model.ToolInfo;
import com.ghost616.agentbase.dto.model.UsageInfo;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.MessageDataProvider.CustomToolCallData;
import com.ghost616.agentbase.service.agent.MessageDataProvider.MessageDTO;
import com.ghost616.agentbase.service.agent.MessageDataProvider.ToolCallData;
import com.ghost616.agentbase.service.agent.MessageDataProvider.WebSearchCallData;
import com.ghost616.agentbase.util.JsonMapper;
import com.ghost616.platform.entity.AgentConfig;
import com.ghost616.platform.entity.Message;
import com.ghost616.platform.entity.MessageToolCall;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.AgentConfigMapper;
import com.ghost616.platform.repository.MessageMapper;
import com.ghost616.platform.repository.MessageToolCallMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.util.IdConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultMessageDataProvider implements MessageDataProvider {

    private static final long AGENT_CONFIG_CACHE_TTL_MILLIS = 60_000L;
    private static final int AGENT_CONFIG_CACHE_MAX_SIZE = 2000;

    private final ConcurrentHashMap<Long, AgentConfigCacheEntry> agentConfigCache = new ConcurrentHashMap<>();

    private final MessageMapper messageMapper;
    private final MessageToolCallMapper messageToolCallMapper;
    private final MessageToolCallService messageToolCallService;
    private final SessionMapper sessionMapper;
    private final AgentConfigMapper agentConfigMapper;

    private static final class AgentConfigCacheEntry {
        private final AgentConfig agentConfig;
        private long expireAt;

        private AgentConfigCacheEntry(AgentConfig agentConfig, long expireAt) {
            this.agentConfig = agentConfig;
            this.expireAt = expireAt;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    @Override
    public String saveMessage(String sessionId, String role, String content, String reasoning,
                               ToolInfo toolInfo, String toolResult, List<ToolCallData> toolCalls,
                               UsageInfo usage, List<WebSearchCallData> webSearchCall, List<CustomToolCallData> customToolCall,
                               String conversationId) {
        Long sid = IdConverter.parse(sessionId);
        Message message = new Message();
        message.setSessionId(sid);
        message.setRole(role);
        message.setContent(content);
        message.setReasoning(reasoning);
        message.setToolCallId(toolInfo != null ? toolInfo.toolCallId() : null);
        message.setToolResult(toolResult);
        message.setConversationId(conversationId);

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
        if (toolInfo != null && toolInfo.toolCallId() != null && !toolInfo.toolCallId().isEmpty()) {
            MessageToolCall mtc = new MessageToolCall();
            mtc.setMessageId(messageId);
            mtc.setToolCallId(toolInfo.toolCallId());
            mtc.setToolCallName(toolInfo.toolName());
            mtc.setType("tool_result");
            batchToolCalls.add(mtc);
        }
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
        TruncationBounds bounds = resolveMemoryTruncationBounds(sid);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, sid)
                .eq(Message::getRollback, false);
        if (bounds != null) {
            wrapper.and(w -> w.lt(Message::getSequenceNum, bounds.firstUserSeq())
                    .or()
                    .ge(Message::getSequenceNum, bounds.keepFromSeq()));
        }
        wrapper.orderByAsc(Message::getSequenceNum);
        List<Message> messages = messageMapper.selectList(wrapper);
        return toMessageDTOs(messages);
    }

    private TruncationBounds resolveMemoryTruncationBounds(Long sessionId) {
        AgentConfig agentConfig = resolveAgentConfig(sessionId);
        if (agentConfig == null || !Boolean.TRUE.equals(agentConfig.getMemoryEnabled())
                || agentConfig.getMemoryGroupCount() == null || agentConfig.getMemoryGroupCount() <= 0) {
            return null;
        }
        Long totalGroups = messageMapper.countUserMessages(sessionId);
        if (totalGroups == null || totalGroups <= agentConfig.getMemoryGroupCount()) {
            return null;
        }
        int skipGroups = totalGroups.intValue() - agentConfig.getMemoryGroupCount();
        Integer firstUserSeq = messageMapper.findNthUserSequenceNum(sessionId, 0);
        Integer keepFromSeq = messageMapper.findNthUserSequenceNum(sessionId, skipGroups);
        if (firstUserSeq == null || keepFromSeq == null) {
            return null;
        }
        return new TruncationBounds(firstUserSeq, keepFromSeq);
    }

    private record TruncationBounds(int firstUserSeq, int keepFromSeq) {}

    private AgentConfig resolveAgentConfig(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        AgentConfigCacheEntry entry = agentConfigCache.get(sessionId);
        if (entry != null && !entry.isExpired()) {
            return entry.agentConfig;
        }
        AgentConfig agentConfig = loadAgentConfig(sessionId);
        putAgentConfigCache(sessionId, agentConfig);
        return agentConfig;
    }

    private void putAgentConfigCache(Long sessionId, AgentConfig agentConfig) {
        if (agentConfigCache.size() >= AGENT_CONFIG_CACHE_MAX_SIZE) {
            agentConfigCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
        agentConfigCache.put(sessionId, new AgentConfigCacheEntry(agentConfig, System.currentTimeMillis() + AGENT_CONFIG_CACHE_TTL_MILLIS));
    }

    private AgentConfig loadAgentConfig(Long sessionId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null || session.getAgentId() == null) {
            return null;
        }
        return agentConfigMapper.selectById(session.getAgentId());
    }

    public List<MessageDTO> toMessageDTOs(List<Message> messages) {
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
                } else if ("tool_result".equals(type)) {
                    continue;
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
                    msg.getReasoning(), buildToolInfo(msg, toolCalls), msg.getSequenceNum(),
                    msg.getCreateTime(), msg.getToolResult(), toolCallDataList, usageInfo,
                    msg.getRollback(), webSearchCallDataList, customToolCallDataList, msg.getConversationId()));
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

    private ToolInfo buildToolInfo(Message msg, List<MessageToolCall> toolCalls) {
        String toolCallId = msg.getToolCallId();
        if (toolCallId == null || toolCallId.isEmpty()) {
            return null;
        }
        for (MessageToolCall tc : toolCalls) {
            if (toolCallId.equals(tc.getToolCallId())) {
                return new ToolInfo(tc.getToolCallId(), tc.getToolCallName());
            }
        }
        return new ToolInfo(toolCallId, null);
    }
}