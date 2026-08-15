package com.ghost616.platform.service.agent_log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghost616.platform.dto.PageResult;
import com.ghost616.platform.dto.agent_log.AgentLogDTO;
import com.ghost616.platform.entity.AgentLogEntity;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.repository.AgentLogMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 智能体日志查询服务实现，提供分页筛选查询与定时清理能力。
 */
@Service
@RequiredArgsConstructor
public class AgentLogServiceImpl implements AgentLogService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final long LOG_RETENTION_DAYS = 30L;

    private final AgentLogMapper agentLogMapper;
    private final SessionMapper sessionMapper;

    @Override
    public PageResult<AgentLogDTO> list(Long sessionId, String sessionName, String conversationId, String logType,
                                        String logLevel, int page, int size) {
        int current = page > 0 ? page : DEFAULT_PAGE;
        int sizeParam = size > 0 ? size : DEFAULT_SIZE;
        Long userId = currentUserId();

        Set<Long> sessionIds = resolveSessionIdsBySessionName(sessionName, userId);
        if (StringUtils.isNotBlank(sessionName) && sessionIds.isEmpty()) {
            return new PageResult<>(List.of(), 0, current, sizeParam);
        }

        LambdaQueryWrapper<AgentLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentLogEntity::getUserId, userId);
        if (sessionId != null) {
            wrapper.eq(AgentLogEntity::getSessionId, sessionId);
        }
        if (StringUtils.isNotBlank(sessionName)) {
            wrapper.in(AgentLogEntity::getSessionId, sessionIds);
        }
        if (StringUtils.isNotBlank(conversationId)) {
            wrapper.eq(AgentLogEntity::getConversationId, conversationId);
        }
        if (StringUtils.isNotBlank(logType)) {
            wrapper.eq(AgentLogEntity::getLogType, logType);
        }
        if (StringUtils.isNotBlank(logLevel)) {
            wrapper.eq(AgentLogEntity::getLogLevel, logLevel);
        }
        wrapper.orderByDesc(AgentLogEntity::getCreateTime);

        Page<AgentLogEntity> queryPage = new Page<>(current, sizeParam);
        agentLogMapper.selectPage(queryPage, wrapper);

        List<AgentLogEntity> records = queryPage.getRecords();
        Map<Long, String> sessionNameMap = loadSessionNames(records);
        List<AgentLogDTO> dtos = records.stream()
                .map(entity -> toDTO(entity, sessionNameMap))
                .toList();
        return PageResult.of(queryPage, dtos);
    }

    private Set<Long> resolveSessionIdsBySessionName(String sessionName, Long userId) {
        if (StringUtils.isBlank(sessionName)) {
            return null;
        }
        LambdaQueryWrapper<Session> sessionWrapper = new LambdaQueryWrapper<>();
        sessionWrapper.eq(Session::getUserId, userId);
        sessionWrapper.like(Session::getTitle, sessionName);
        return sessionMapper.selectList(sessionWrapper).stream()
                .map(Session::getId)
                .collect(Collectors.toSet());
    }

    @Override
    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanupExpiredLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(LOG_RETENTION_DAYS);
        LambdaQueryWrapper<AgentLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(AgentLogEntity::getCreateTime, cutoff);
        agentLogMapper.delete(wrapper);
    }

    private Map<Long, String> loadSessionNames(List<AgentLogEntity> records) {
        Set<Long> sessionIds = records.stream()
                .map(AgentLogEntity::getSessionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (sessionIds.isEmpty()) {
            return Map.of();
        }
        List<Session> sessions = sessionMapper.selectBatchIds(sessionIds);
        return sessions.stream()
                .collect(Collectors.toMap(Session::getId, this::resolveSessionName, (a, b) -> a));
    }

    private String resolveSessionName(Session session) {
        String title = session.getTitle();
        if (title == null || title.isEmpty()) {
            return String.valueOf(session.getId());
        }
        return title;
    }

    private AgentLogDTO toDTO(AgentLogEntity entity, Map<Long, String> sessionNameMap) {
        return AgentLogDTO.builder()
                .id(entity.getId())
                .sessionId(entity.getSessionId())
                .sessionName(entity.getSessionId() != null ? sessionNameMap.get(entity.getSessionId()) : null)
                .conversationId(entity.getConversationId())
                .logType(entity.getLogType())
                .logLevel(entity.getLogLevel())
                .logData(entity.getLogData())
                .sessionVariables(entity.getSessionVariables())
                .conversationVariables(entity.getConversationVariables())
                .createTime(entity.getCreateTime())
                .build();
    }

    /**
     * 获取当前登录用户 ID。
     *
     * <p>从 {@link UserContext} 线程上下文读取用户会话；
     * 未登录时抛出 {@link ErrorCode#USER_NOT_LOGIN}，防止越权查询其他用户日志。</p>
     *
     * @return 当前登录用户 ID
     */
    private Long currentUserId() {
        UserSession session = UserContext.get();
        if (session == null || session.getUser() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        return session.getUser().getId();
    }
}
