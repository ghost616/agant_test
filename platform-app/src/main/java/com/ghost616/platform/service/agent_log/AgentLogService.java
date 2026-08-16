package com.ghost616.platform.service.agent_log;

import com.ghost616.platform.dto.PageResult;
import com.ghost616.platform.dto.agent_log.AgentLogDTO;

/**
 * 智能体日志查询服务接口，提供列表查询、筛选、分页及定时清理能力。
 */
public interface AgentLogService {

    /**
     * 分页查询智能体日志，支持按主会话（含子会话）/会话名/会话/对话/类型/级别筛选，返回会话名。
     *
     * @param sessionId      会话 ID（可空）
     * @param rootSessionId  主会话 ID（可空），非空时按该主会话及其所有子会话的 sessionId 集合 in 过滤
     * @param sessionName    会话名模糊搜索（可空）
     * @param conversationId 对话 ID（可空）
     * @param logType        日志类型（可空）
     * @param logLevel       日志级别（可空）
     * @param page           页码，从 1 开始
     * @param size           每页大小
     * @return 分页结果
     */
    PageResult<AgentLogDTO> list(Long sessionId, Long rootSessionId, String sessionName, String conversationId,
                                 String logType, String logLevel, int page, int size);

    /**
     * 清理 30 天前的日志记录，供定时任务调用。
     */
    void cleanupExpiredLogs();
}
