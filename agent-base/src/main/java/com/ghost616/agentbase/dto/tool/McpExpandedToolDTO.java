package com.ghost616.agentbase.dto.tool;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class McpExpandedToolDTO extends ToolConfigDTO {

    private String remoteToolName;
}
