package com.ghost616.platform.service.agent_log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghost616.platform.dto.PageResult;
import com.ghost616.platform.dto.agent_log.AgentLogDTO;
import com.ghost616.platform.entity.AgentLogEntity;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.AgentLogMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.session.UserContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
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
    public PageResult<AgentLogDTO> list(Long sessionId, Long rootSessionId, String sessionName, String conversationId,
                                        String logType, String logLevel, int page, int size) {
        int current = page > 0 ? page : DEFAULT_PAGE;
        int sizeParam = size > 0 ? size : DEFAULT_SIZE;
        Long userId = UserContextUtil.requireUserId();

        Set<Long> sessionIds = resolveSessionIdsBySessionName(sessionName, userId);
        if (StringUtils.isNotBlank(sessionName) && sessionIds.isEmpty()) {
            return new PageResult<>(List.of(), 0, current, sizeParam);
        }

        Set<Long> rootSessionIds = resolveSessionIdsByRootSession(rootSessionId, userId);
        if (rootSessionId != null && rootSessionIds.isEmpty()) {
            return new PageResult<>(List.of(), 0, current, sizeParam);
        }

        LambdaQueryWrapper<AgentLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentLogEntity::getUserId, userId);
        if (sessionId != null) {
            wrapper.eq(AgentLogEntity::getSessionId, sessionId);
        }
        if (rootSessionId != null) {
            wrapper.in(AgentLogEntity::getSessionId, rootSessionIds);
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
        Map<Long, Session> sessionMap = loadSessions(records);
        List<AgentLogDTO> dtos = records.stream()
                .map(entity -> toDTO(entity, sessionMap))
                .toList();
        return PageResult.of(queryPage, dtos);
    }

    /**
     * 解析主会话及其所有子会话的 sessionId 集合（含主会话自身）。
     * 主会话不存在或不属于当前用户时返回空集合；rootSessionId 为 null 时返回 null（不追加过滤）。
     */
    private Set<Long> resolveSessionIdsByRootSession(Long rootSessionId, Long userId) {
        if (rootSessionId == null) {
            return null;
        }
        Session root = sessionMapper.selectById(rootSessionId);
        if (root == null || !Objects.equals(root.getUserId(), userId)) {
            return Set.of();
        }
        Set<Long> ids = new HashSet<>();
        ids.add(rootSessionId);

        LambdaQueryWrapper<Session> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(Session::getParentSessionId, rootSessionId);
        childWrapper.eq(Session::getIsChild, true);
        List<Session> children = sessionMapper.selectList(childWrapper);
        for (Session child : children) {
            ids.add(child.getId());
        }
        return ids;
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

    private Map<Long, Session> loadSessions(List<AgentLogEntity> records) {
        Set<Long> sessionIds = records.stream()
                .map(AgentLogEntity::getSessionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (sessionIds.isEmpty()) {
            return Map.of();
        }
        List<Session> sessions = sessionMapper.selectBatchIds(sessionIds);
        return sessions.stream()
                .collect(Collectors.toMap(Session::getId, s -> s, (a, b) -> a));
    }

    private String resolveSessionName(Session session) {
        String title = session.getTitle();
        if (title == null || title.isEmpty()) {
            return String.valueOf(session.getId());
        }
        return title;
    }

    private AgentLogDTO toDTO(AgentLogEntity entity, Map<Long, Session> sessionMap) {
        Session session = entity.getSessionId() != null ? sessionMap.get(entity.getSessionId()) : null;
        return AgentLogDTO.builder()
                .id(entity.getId())
                .sessionId(entity.getSessionId())
                .sessionName(session != null ? resolveSessionName(session) : null)
                .isChild(session != null ? session.getIsChild() : null)
                .conversationId(entity.getConversationId())
                .logType(entity.getLogType())
                .logLevel(entity.getLogLevel())
                .logData(entity.getLogData())
                .sessionVariables(entity.getSessionVariables())
                .conversationVariables(entity.getConversationVariables())
                .createTime(entity.getCreateTime())
                .build();
    }
}
