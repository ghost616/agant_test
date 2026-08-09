package com.ghost616.platform.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.log.AgentLog;
import com.ghost616.agentbase.service.agent.log.ContextLogData;
import com.ghost616.agentbase.service.agent.log.LogData;
import com.ghost616.platform.entity.AgentLogEntity;
import com.ghost616.platform.repository.AgentLogMapper;
import com.ghost616.platform.util.IdConverter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 基于数据库的 AgentLog 实现，将智能体日志持久化到 agent_log 表。
 */
@Service
@RequiredArgsConstructor
public class DatabaseAgentLog implements AgentLog {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAgentLog.class);

    private final AgentLogMapper agentLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void addLog(LogData logData) {
        AgentLogEntity entity = new AgentLogEntity();
        if (logData instanceof ContextLogData contextLogData) {
            AgentExecutionContext context = contextLogData.getContext();
            if (context != null) {
                entity.setSessionId(parseSessionId(context.getSessionId()));
                entity.setConversationId(context.getConversationId());
            }
        }
        entity.setLogType(logData.logType().getCode());
        if (logData.getLogLevel() != null) {
            entity.setLogLevel(logData.getLogLevel().getCode());
        }
        entity.setLogData(serializeLogData(logData));
        agentLogMapper.insert(entity);
    }

    private Long parseSessionId(String sessionId) {
        try {
            return IdConverter.parse(sessionId);
        } catch (IllegalArgumentException e) {
            log.warn("解析日志会话 ID 失败: {}", sessionId);
            return null;
        }
    }

    private String serializeLogData(LogData logData) {
        try {
            return objectMapper.writeValueAsString(logData);
        } catch (Exception e) {
            log.warn("序列化日志数据失败: {}", e.getMessage());
            return null;
        }
    }
}
