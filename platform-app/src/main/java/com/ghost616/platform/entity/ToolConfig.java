package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ToolType;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_config")
public class ToolConfig extends BaseEntity {

    private String name;

    @TableField("tool_type")
    private ToolType toolType;

    private String description;

    @TableField("parameter_schema")
    private String parameterSchema;

    @TableField("return_schema")
    private String returnSchema;

    @TableField("impl_path")
    private String implPath;

    @TableField("auth_config")
    private String authConfig;

    private CommonStatus status;
}
