package com.ghost616.agentbase.service.agent.log;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 携带会话级别信息的日志数据基类。
 */
@Getter
@SuperBuilder
public abstract class SessionLogData extends LogData {

    /** 会话 ID */
    private final String sessionId;

    /** 对话 ID */
    private final String conversationId;
}
