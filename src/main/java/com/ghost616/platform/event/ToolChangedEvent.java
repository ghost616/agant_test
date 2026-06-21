package com.ghost616.platform.event;

import org.springframework.context.ApplicationEvent;

public class ToolChangedEvent extends ApplicationEvent {
    private final Long toolId;

    public ToolChangedEvent(Object source, Long toolId) {
        super(source);
        this.toolId = toolId;
    }

    public Long getToolId() {
        return toolId;
    }
}
