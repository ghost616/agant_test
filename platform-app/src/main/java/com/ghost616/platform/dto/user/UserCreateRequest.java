package com.ghost616.platform.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加用户请求体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

    @NotBlank(message = "登录名不能为空")
    private String loginName;

    private String displayName;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 登录开关：0 禁止登录，1 允许登录（默认）。 */
    private Integer enabled;
}
