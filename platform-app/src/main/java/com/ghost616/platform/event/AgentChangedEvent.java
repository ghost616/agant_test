package com.ghost616.platform.event;

import org.springframework.context.ApplicationEvent;

/**
 * 智能体配置变更事件，携带发生变更的智能体 ID。
 * 用于通知缓存类组件（如 SubSessionWebSocketModeResolver）在智能体配置更新后失效相关缓存。
 */
public class AgentChangedEvent extends ApplicationEvent {

    private final Long agentId;

    public AgentChangedEvent(Object source, Long agentId) {
        super(source);
        this.agentId = agentId;
    }

    public Long getAgentId() {
        return agentId;
    }
}