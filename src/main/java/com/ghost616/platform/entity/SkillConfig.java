package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ghost616.platform.enums.CommonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("skill_config")
public class SkillConfig extends BaseEntity {

    private String name;

    private String description;

    private String prompt;

    private CommonStatus status;
}
