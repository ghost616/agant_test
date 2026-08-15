package com.ghost616.platform.dto.user;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ghost616.platform.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息响应体，不包含密码。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String loginName;

    private String displayName;

    /** 用户类型：1 普通用户，2 管理员。 */
    private Integer userType;

    /** 登录开关：0 禁止登录，1 允许登录。 */
    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 从用户实体构建 DTO（剔除密码字段）。
     *
     * @param user 用户实体
     * @return 用户 DTO，实体为 null 时返回 null
     */
    public static UserDTO of(User user) {
        if (user == null) {
            return null;
        }
        return UserDTO.builder()
                .id(user.getId())
                .loginName(user.getLoginName())
                .displayName(user.getDisplayName())
                .userType(user.getUserType())
                .enabled(user.getEnabled())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }
}
