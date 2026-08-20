package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.enums.SubSessionOpenMode;
import com.ghost616.platform.entity.AgentConfig;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.AgentConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.util.IdConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 子会话 WEBSOCKET 打开方式解析器。
 * 判断指定会话是否为「WEBSOCKET 打开方式」的子会话：isChild=true 且沿 parentSessionId 链
 * 解析到主会话（无父会话的根），主会话 agentId 对应 AgentConfig.subSessionOpenMode==WEBSOCKET 时返回 true。
 * 解析结果按 sessionId 缓存于 ConcurrentHashMap，容量上限 10000，超限时清空整个缓存；
 * 智能体配置变更（AgentChangedEvent）时由监听方调用 clearCache() 失效缓存。
 */
@Component
@RequiredArgsConstructor
public class SubSessionWebSocketModeResolver {

    private static final int MAX_CACHE_SIZE = 10000;

    private final SessionMapper sessionMapper;
    private final AgentConfigMapper agentConfigMapper;

    private final ConcurrentHashMap<String, Boolean> cache = new ConcurrentHashMap<>();

    /**
     * 判断指定会话是否为 WEBSOCKET 打开方式的子会话（结果缓存）。
     *
     * @param sessionId 会话 ID（String）
     * @return true 表示 WEBSOCKET 子会话；会话缺失、非子会话、主会话解析失败或配置缺失时返回 false
     */
    public boolean isWebSocketSubSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        Boolean cached = cache.get(sessionId);
        if (cached != null) {
            return cached;
        }
        boolean result = resolve(sessionId);
        if (cache.size() >= MAX_CACHE_SIZE) {
            cache.clear();
        }
        cache.put(sessionId, result);
        return result;
    }

    /**
     * 清空全部缓存（智能体配置变更后调用）。
     */
    public void clearCache() {
        cache.clear();
    }

    private boolean resolve(String sessionId) {
        Long sid = IdConverter.parse(sessionId);
        if (sid == null) {
            return false;
        }
        Session session = sessionMapper.selectById(sid);
        if (session == null || !Boolean.TRUE.equals(session.getIsChild()) || session.getParentSessionId() == null) {
            return false;
        }
        Long mainSessionId = resolveMainSessionId(session);
        if (mainSessionId == null) {
            return false;
        }
        Session mainSession = sessionMapper.selectById(mainSessionId);
        if (mainSession == null || mainSession.getAgentId() == null) {
            return false;
        }
        AgentConfig agentConfig = agentConfigMapper.selectById(mainSession.getAgentId());
        return agentConfig != null && SubSessionOpenMode.WEBSOCKET == agentConfig.getSubSessionOpenMode();
    }

    /**
     * 从子会话的 parentSessionId 出发，沿 parentSessionId 链解析主会话（无父会话的根）。
     *
     * @return 主会话 ID；解析失败（会话缺失或出现环）时返回 null
     */
    private Long resolveMainSessionId(Session childSession) {
        Long current = childSession.getParentSessionId();
        Set<Long> visited = new HashSet<>();
        while (current != null && visited.add(current)) {
            Session session = sessionMapper.selectById(current);
            if (session == null) {
                return null;
            }
            if (session.getParentSessionId() == null) {
                return current;
            }
            current = session.getParentSessionId();
        }
        return null;
    }
}