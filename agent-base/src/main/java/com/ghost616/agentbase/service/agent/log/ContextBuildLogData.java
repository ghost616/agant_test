package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * 智能体上下文构建日志数据，记录会话上下文构建过程中的关键信息。
 */
@Getter
@SuperBuilder
public class ContextBuildLogData extends LogData {

    /** 会话 ID */
    private final String sessionId;

    /** 智能体 ID */
    private final String agentId;

    /** 模型 ID */
    private final String modelId;

    /** 工具数量 */
    private final int toolCount;

    /** 历史消息数量 */
    private final int historyCount;

    /** 是否为子会话 */
    private final boolean isSubSession;

    /** 是否命中缓存 */
    private final boolean cacheHit;

    /** 会话变量（防御性复制，避免影响 AgentSessionContext） */
    private final Map<String, String> sessionVariables;

    @Override
    public LogType logType() {
        return LogType.CONTEXT_BUILD;
    }
}
