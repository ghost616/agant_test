package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("session_variable")
public class SessionVariable extends BaseEntity {

    @TableField("session_id")
    private Long sessionId;

    @TableField("variable_key")
    private String variableKey;

    @TableField("variable_value")
    private String variableValue;
}
