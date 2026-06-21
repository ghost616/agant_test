package com.ghost616.platform.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentUpdateRequest {

    private String name;

    private String description;

    private String systemPrompt;

    private Long modelId;

    private List<Long> toolIds;
}
