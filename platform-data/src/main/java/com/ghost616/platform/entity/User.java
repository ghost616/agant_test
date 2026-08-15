package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体，映射 user 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    @TableField("login_name")
    private String loginName;

    @TableField("display_name")
    private String displayName;

    @TableField("user_type")
    private Integer userType;

    private String password;

    private Integer enabled = 1;
}
