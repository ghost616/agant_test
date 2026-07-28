package com.ghost616.platform.dto.evaluation;

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
public class EvaluationDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;

    private String description;

    private Integer executionCount;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long modelId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long agentEvalId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long agentId;

    private String agentName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long benchmarkSessionId;

    private String executionType;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
