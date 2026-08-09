package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 智能体请求入口日志数据。
 */
@Getter
@SuperBuilder
public class RequestEntryLogData extends ContextLogData {

    /** 会话 ID */
    private final String sessionId;

    /** 模型 ID */
    private final String modelId;

    /** 用户请求内容 */
    private final String content;

    /** 是否为工具调用继续 */
    private final Boolean isToolContinue;

    @Override
    public LogType logType() {
        return LogType.REQUEST_ENTRY;
    }
}
