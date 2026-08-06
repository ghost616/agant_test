package com.ghost616.platform.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 知识库发布状态枚举。
 */
public enum PublishStatus {

    UNPUBLISHED("UNPUBLISHED", "未发布"),
    PUBLISHING("PUBLISHING", "正在发布"),
    PUBLISHED("PUBLISHED", "已发布"),
    PENDING_PUBLISH("PENDING_PUBLISH", "待发布（已发布后内容被修改）"),
    PUBLISH_ERROR("PUBLISH_ERROR", "发布错误");

    @EnumValue
    private final String code;
    private final String description;

    PublishStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
