package com.ghost616.platform.dto.skill;

import jakarta.validation.constraints.NotBlank;
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
public class SkillCreateRequest {

    @NotBlank(message = "SKILL 名称不能为空")
    @Pattern(regexp = "^(?!_sys_)[a-z0-9_]+$", message = "SKILL 名称仅允许小写字母、数字、下划线，且不允许以 _sys_ 开头")
    private String name;

    private String description;

    @NotBlank(message = "提示词不能为空")
    private String prompt;

    private List<Long> toolIds;
}
