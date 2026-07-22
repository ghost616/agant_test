package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.dto.model.ChatChunk;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class HookData {

    private final ChatChunk chatChunk;
    private final ToolHookContext toolContext;

    public HookData(ChatChunk chatChunk) {
        this.chatChunk = chatChunk;
        this.toolContext = null;
    }

    public HookData(ToolHookContext toolContext) {
        this.chatChunk = null;
        this.toolContext = toolContext;
    }
}
