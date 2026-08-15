package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.ghost616.agentbase.enums.CommonStatus;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("skill_config")
public class SkillConfig extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    private String name;

    private String description;

    private String prompt;

    private CommonStatus status;
}
