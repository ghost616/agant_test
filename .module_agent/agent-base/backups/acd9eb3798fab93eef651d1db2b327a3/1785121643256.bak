package com.ghost616.agentbase.dto.skill;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.SessionAuthType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillConfigDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;

    private String description;

    private String prompt;

    private CommonStatus status;

    private SessionAuthType sessionAuth;

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> toolIds;

    private List<ToolConfigDTO> skillTools;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
