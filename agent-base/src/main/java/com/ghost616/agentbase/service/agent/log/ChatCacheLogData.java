package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 智能体对话数据缓存日志数据，记录聊天数据缓存生命周期操作的关键信息。
 */
@Getter
@SuperBuilder
public class ChatCacheLogData extends SessionLogData {

    /** 缓存 ID */
    private final String cacheId;

    /** 缓存操作类型（CACHE_START/CACHE_APPEND/CACHE_REMOVE/CACHE_STREAM） */
    private final String operation;

    /** 本次拉取起始序号（轮询拉取新数据时使用，其他操作可为 null） */
    private final Integer from;

    /** 本次拉取到的最大序号（轮询拉取新数据时使用，其他操作可为 null） */
    private final Integer currentMax;

    @Override
    public LogType logType() {
        return LogType.CHAT_CACHE;
    }
}
