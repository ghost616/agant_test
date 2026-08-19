package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 父会话消息发送日志数据，记录子会话向父会话发送用户消息的关键信息。
 * sessionId 为当前会话 ID（调用方子会话），conversationId 由基类 SessionLogData 承载。
 */
@Getter
@SuperBuilder
public class SendParentMessageLogData extends SessionLogData {

    /** 父会话 ID（消息发送目标） */
    private final String parentSessionId;

    /** 消息内容 */
    private final String content;

    @Override
    public LogType logType() {
        return LogType.SEND_PARENT_MESSAGE;
    }
}