package com.ghost616.platform.controller;

import com.ghost616.platform.dto.agent_log.AgentLogDTO;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.PageResult;
import com.ghost616.platform.service.agent_log.AgentLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能体日志查询 REST 控制器。
 */
@RestController
@RequestMapping("/api/agent-logs")
@RequiredArgsConstructor
public class AgentLogController {

    private final AgentLogService agentLogService;

    @GetMapping
    public ApiResponse<PageResult<AgentLogDTO>> list(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long rootSessionId,
            @RequestParam(required = false) String sessionName,
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) String logLevel,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<AgentLogDTO> result = agentLogService.list(sessionId, rootSessionId, sessionName, conversationId, logType, logLevel, page, size);
        return ApiResponse.success(result);
    }
}
