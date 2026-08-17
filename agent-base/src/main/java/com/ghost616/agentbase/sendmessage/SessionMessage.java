package com.ghost616.agentbase.sendmessage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class SessionMessage implements MessageDefinition {

    private String sessionId;

    /** 对话 ID（消息所属对话标识） */
    private String conversationId;

    /** 父会话 ID（子会话消息携带直接父会话，主会话为 null） */
    private String parentSessionId;

    /** 主会话 ID（沿父会话链向上追溯到无父会话的根会话） */
    private String mainSessionId;
}
