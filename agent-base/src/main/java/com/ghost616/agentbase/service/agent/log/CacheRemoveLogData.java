package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 智能体缓存移除日志数据，记录会话上下文从缓存移除的信息。
 */
@Getter
@SuperBuilder
public class CacheRemoveLogData extends SessionLogData {

    @Override
    public LogType logType() {
        return LogType.CACHE_REMOVE;
    }
}
