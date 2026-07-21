package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.dto.model.ChatChunk;
import lombok.Data;

@Data
public class HookData {

    private final ChatChunk chatChunk;
}
