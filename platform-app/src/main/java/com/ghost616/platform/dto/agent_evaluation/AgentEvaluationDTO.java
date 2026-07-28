package com.ghost616.platform.dto.agent_evaluation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEvaluationDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;

    private String description;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long agentId;

    private String agentName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
