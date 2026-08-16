package com.ghost616.platform.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户自助修改请求体（当前登录用户修改自己的显示名/密码），字段为空表示不修改。
 *
 * <p>登录开关（enabled）不允许用户自助修改，故不包含该字段。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSelfUpdateRequest {

    /** 新显示名，为空表示不修改。 */
    private String displayName;

    /** 新密码，为空表示不修改。 */
    private String password;
}
