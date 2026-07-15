package com.ghost616.platform.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.ghost616.platform.dto.agent.AgentToolItem;
import com.ghost616.platform.dto.agent.AgentSkillItem;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentUpdateRequest {

    private String name;

    private String description;

    private String systemPrompt;

    private Long modelId;

    private Integer recentMessageCount;

    private List<AgentToolItem> tools;

    private List<AgentSkillItem> skills;
}
