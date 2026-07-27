package com.ghost616.agentbase.event;

import org.springframework.context.ApplicationEvent;

public class ToolChangedEvent extends ApplicationEvent {
    private final String toolId;

    public ToolChangedEvent(Object source, String toolId) {
        super(source);
        this.toolId = toolId;
    }

    public String getToolId() {
        return toolId;
    }
}
