package com.ghost616.platform.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改用户请求体，字段为空表示不修改。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    private String displayName;

    /** 用户类型：1 普通用户，2 管理员。 */
    private Integer userType;

    /** 新密码，为空表示不修改。 */
    private String password;

    /** 登录开关：0 禁止登录，1 允许登录。 */
    private Integer enabled;
}
