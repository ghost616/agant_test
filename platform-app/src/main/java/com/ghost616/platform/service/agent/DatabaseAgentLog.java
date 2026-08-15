package com.ghost616.platform.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.enums.LogType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.log.AgentLog;
import com.ghost616.agentbase.service.agent.log.ContextBuildLogData;
import com.ghost616.agentbase.service.agent.log.ContextLogData;
import com.ghost616.agentbase.service.agent.log.LogData;
import com.ghost616.agentbase.service.agent.log.SessionLogData;
import com.ghost616.platform.entity.AgentLogEntity;
import com.ghost616.platform.repository.AgentLogMapper;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import com.ghost616.platform.util.IdConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 基于数据库的 AgentLog 实现，将智能体日志持久化到 agent_log 表。
 */
@Service
public class DatabaseAgentLog implements AgentLog {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAgentLog.class);

    private final AgentLogMapper agentLogMapper;
    private final ObjectMapper objectMapper;

    public DatabaseAgentLog(AgentLogMapper agentLogMapper, ObjectMapper objectMapper) {
        this.agentLogMapper = agentLogMapper;
        this.objectMapper = objectMapper.copy()
                .addMixIn(LogData.class, LogDataMixin.class)
                .addMixIn(ContextLogData.class, ContextLogDataMixin.class)
                .addMixIn(SessionLogData.class, SessionLogDataMixin.class);
    }

    @Override
    public void addLog(LogData logData) {
        AgentLogEntity entity = new AgentLogEntity();
        entity.setUserId(currentUserId());
        if (logData instanceof ContextLogData contextLogData) {
            AgentExecutionContext context = contextLogData.getContext();
            if (context != null) {
                entity.setSessionId(parseSessionId(context.getSessionId()));
                entity.setConversationId(context.getConversationId());
                entity.setSessionVariables(serializeVariables(
                        buildVariables(context.getSessionVariableKeys(), context::getSessionVariable)));
                entity.setConversationVariables(serializeVariables(
                        buildVariables(context.getConversationVariableKeys(), context::getConversationVariable)));
            }
        } else if (logData instanceof SessionLogData sessionLogData) {
            entity.setSessionId(parseSessionId(sessionLogData.getSessionId()));
            entity.setConversationId(sessionLogData.getConversationId());
        } else if (logData.logType() == LogType.CONTEXT_BUILD) {
            entity.setSessionId(parseSessionId(((ContextBuildLogData) logData).getSessionId()));
        }
        entity.setLogType(logData.logType().getCode());
        if (logData.getLogLevel() != null) {
            entity.setLogLevel(logData.getLogLevel().getCode());
        }
        entity.setLogData(serializeLogData(logData));
        agentLogMapper.insert(entity);
    }

    private Map<String, String> buildVariables(Set<String> keys, Function<String, String> valueGetter) {
        Map<String, String> variables = new HashMap<>();
        for (String key : keys) {
            variables.put(key, valueGetter.apply(key));
        }
        return variables;
    }

    private String serializeVariables(Map<String, String> variables) {
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (Exception e) {
            log.warn("序列化会话变量失败: {}", e.getMessage());
            return null;
        }
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

    /**
     * 获取当前登录用户 ID。
     *
     * <p>从 {@link UserContext} 线程上下文读取用户会话；
     * 异步场景（如工具异步执行线程）通过线程变量传播保证上下文可取。
     * 无用户上下文（如系统级流程）时返回 null，userId 留空，避免中断系统级流程。</p>
     *
     * @return 当前登录用户 ID，无用户上下文时返回 null
     */
    private Long currentUserId() {
        UserSession session = UserContext.get();
        if (session == null || session.getUser() == null) {
            return null;
        }
        return session.getUser().getId();
    }
}
