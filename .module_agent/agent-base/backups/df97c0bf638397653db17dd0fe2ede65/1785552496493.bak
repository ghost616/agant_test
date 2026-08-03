package com.ghost616.agentbase.service.agent;

import java.time.LocalDateTime;
import java.util.List;

import com.ghost616.agentbase.dto.model.UsageInfo;

public interface MessageDataProvider {

    String saveMessage(String sessionId, String role, String content, String reasoning,
                       String toolCallId, String toolResult, List<ToolCallData> toolCalls,
                       UsageInfo usage);

    List<MessageDTO> getMessages(String sessionId);

    int rollbackToLastUserMessage(String sessionId);

    record ToolCallData(String toolCallId, String toolCallName, String toolCallArguments) {
    }

    record MessageDTO(String id, String sessionId, String role, String content, String reasoning,
                      String toolCallId, Integer sequenceNum, LocalDateTime createTime,
                      String toolResult, List<ToolCallData> toolCalls, UsageInfo usage,
                      Boolean rollback) {
    }
}
