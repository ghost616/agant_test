package com.ghost616.platform.dto.skill;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ghost616.platform.enums.CommonStatus;
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

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> toolIds;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
