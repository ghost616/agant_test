package com.ghost616.platform.dto.skill;

import com.ghost616.platform.enums.CommonStatus;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillUpdateRequest {

    @Pattern(regexp = "^[a-z0-9_]+$", message = "SKILL 名称仅允许小写字母、数字和下划线")
    private String name;

    private String description;

    private String prompt;

    private CommonStatus status;

    private List<Long> toolIds;
}
