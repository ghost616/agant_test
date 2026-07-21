package com.ghost616.agentbase.service.agent.invoker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolHookContext {

    private String toolCallId;
    private String toolName;
    private String arguments;
    private String result;
}
