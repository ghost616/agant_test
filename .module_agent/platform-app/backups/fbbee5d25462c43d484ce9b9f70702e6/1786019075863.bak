package com.ghost616.platform.dto.agent;

import jakarta.validation.constraints.NotBlank;
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
public class AgentCreateRequest {

    @NotBlank(message = "智能体名称不能为空")
    private String name;

    private String description;

    private String systemPrompt;

    private Long modelId;

    private Integer recentMessageCount;

    private List<AgentToolItem> tools;

    private List<AgentSkillItem> skills;
}
