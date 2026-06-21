package com.ghost616.platform.dto.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatChunk {

    private String delta;

    private String reasoning;

    private List<ToolCallDelta> toolCalls;

    private String finishReason;

    private Boolean hasToolCalls;

    private Integer index;
}
