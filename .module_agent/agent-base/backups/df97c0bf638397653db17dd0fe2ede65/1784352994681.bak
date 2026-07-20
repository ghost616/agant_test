package com.ghost616.agentbase.service.agent;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageDataProvider {

    Long saveMessage(Long sessionId, String role, String content, String reasoning,
                     String toolCallId, String toolResult, List<ToolCallData> toolCalls);

    List<MessageDTO> getMessages(Long sessionId);

    int rollbackToLastUserMessage(Long sessionId);

    record ToolCallData(String toolCallId, String toolCallName, String toolCallArguments) {
    }

    record MessageDTO(Long id, Long sessionId, String role, String content, String reasoning,
                      String toolCallId, Integer sequenceNum, LocalDateTime createTime,
                      String toolResult, List<ToolCallData> toolCalls) {
    }
}
