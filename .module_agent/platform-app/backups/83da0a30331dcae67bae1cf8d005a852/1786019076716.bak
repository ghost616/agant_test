package com.ghost616.platform.dto.agent;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.dto.agent.AgentToolItem;
import com.ghost616.platform.dto.agent.AgentSkillItem;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfigDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String name;
    private String description;
    private String systemPrompt;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long modelId;
    private CommonStatus status;
    private Integer recentMessageCount;
    private List<AgentToolItem> tools;
    private List<AgentSkillItem> skills;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
