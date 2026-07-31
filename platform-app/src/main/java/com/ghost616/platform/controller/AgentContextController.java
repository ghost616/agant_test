package com.ghost616.platform.controller;

import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.platform.dto.AgentContextBasicDTO;
import com.ghost616.platform.dto.AgentContextDTO;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.context.ConversationVariableRequest;
import com.ghost616.platform.dto.context.SessionVariableRequest;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.util.IdConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/context")
@RequiredArgsConstructor
public class AgentContextController {

    private final AgentContextManager agentContextManager;
    private final SessionMapper sessionMapper;

    @GetMapping("/{sessionId}")
    public ApiResponse<AgentContextDTO> getContext(@PathVariable Long sessionId) {
        AgentContextManager.AgentSessionContext sessionContext = agentContextManager.get(IdConverter.toString(sessionId));
        if (sessionContext == null) {
            return ApiResponse.fail("CONTEXT-001", "Session context not found: " + sessionId);
        }
        AgentExecutionContext ctx = sessionContext.context();

        AgentContextDTO dto = new AgentContextDTO();
        dto.setSessionId(IdConverter.parse(ctx.getSessionId()));
        dto.setAgentId(IdConverter.parse(ctx.getAgentId()));
        dto.setSystemPrompt(ctx.getSystemPrompt());
        dto.setModelId(IdConverter.parse(ctx.getModelId()));
        dto.setParentSessionId(IdConverter.parse(ctx.getParentSessionId()));
        dto.setRecentMessageCount(ctx.getRecentMessageCount());
        dto.setHistory(mapHistory(ctx.getHistory()));
        dto.setTools(ctx.getTools());
        dto.setSkills(ctx.getSkills());
        dto.setProjectDir(ctx.getProjectDir());
        dto.setLastResponseId(ctx.getLastResponseId());
        dto.setSessionVariables(ctx.getSessionVariableKeys().stream()
                .collect(java.util.stream.Collectors.toMap(k -> k, ctx::getSessionVariable)));
        dto.setConversationVariables(ctx.getConversationVariableKeys().stream()
                .collect(java.util.stream.Collectors.toMap(k -> k, ctx::getConversationVariable)));

        return ApiResponse.success(dto);
    }

    @GetMapping("/{sessionId}/basic")
    public ApiResponse<AgentContextBasicDTO> getContextBasic(@PathVariable Long sessionId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return ApiResponse.fail("CONTEXT-001", "Session not found: " + sessionId);
        }
        AgentContextBasicDTO dto = new AgentContextBasicDTO();
        dto.setSessionId(session.getId());
        dto.setAgentId(session.getAgentId());
        dto.setModelId(session.getModelId());
        dto.setLastResponseId(session.getLastResponseId());
        dto.setParentSessionId(session.getParentSessionId());
        return ApiResponse.success(dto);
    }

    @PostMapping("/{sessionId}/session-variable")
    public ApiResponse<Void> putSessionVariable(@PathVariable Long sessionId,
                                                  @RequestBody SessionVariableRequest body) {
        AgentContextManager.AgentSessionContext sessionContext = agentContextManager.get(IdConverter.toString(sessionId));
        if (sessionContext == null) {
            return ApiResponse.fail("CONTEXT-001", "Session context not found: " + sessionId);
        }
        sessionContext.context().putSessionVariable(body.getKey(), body.getValue());
        return ApiResponse.success(null);
    }

    @PostMapping("/{sessionId}/conversation-variable")
    public ApiResponse<Void> putConversationVariable(@PathVariable Long sessionId,
                                                        @RequestBody ConversationVariableRequest body) {
        AgentContextManager.AgentSessionContext sessionContext = agentContextManager.get(IdConverter.toString(sessionId));
        if (sessionContext == null) {
            return ApiResponse.fail("CONTEXT-001", "Session context not found: " + sessionId);
        }
        sessionContext.context().putConversationVariable(body.getKey(), body.getValue());
        return ApiResponse.success(null);
    }

    private List<AgentContextDTO.HistoryEntryDTO> mapHistory(List<AgentExecutionContext.HistoryEntry> history) {
        return history.stream().map(entry -> {
            AgentContextDTO.HistoryEntryDTO dto = new AgentContextDTO.HistoryEntryDTO();
            dto.setRole(entry.role());
            dto.setContent(entry.content());
            dto.setReasoning(entry.reasoning());
            dto.setToolCallId(entry.toolCallId());
            dto.setSequenceNum(entry.sequenceNum());
            dto.setCreateTime(entry.createTime());
            dto.setToolCalls(entry.toolCalls());
            dto.setUsage(entry.usage());
            return dto;
        }).toList();
    }
}
