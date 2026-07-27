package com.ghost616.agentbase.dto.skill;

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

    private String id;

    private String name;

    private String description;

    private String prompt;

    private CommonStatus status;

    private SessionAuthType sessionAuth;

    private List<String> toolIds;

    private List<ToolConfigDTO> skillTools;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
