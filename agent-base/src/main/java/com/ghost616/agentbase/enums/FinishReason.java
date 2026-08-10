package com.ghost616.agentbase.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 模型响应结束原因枚举，定义流式/非流式响应的结束方式。
 */
public enum FinishReason {

    STOP("stop", "正常结束"),
    LENGTH("length", "达到长度限制"),
    TOOL_CALLS("tool_calls", "触发工具调用"),
    CONTENT_FILTER("content_filter", "内容被过滤"),
    ERROR("error", "发生错误"),
    CANCELLED("cancelled", "被取消");

    @EnumValue
    private final String code;
    private final String description;

    FinishReason(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static FinishReason fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (FinishReason reason : values()) {
            if (reason.code.equals(code)) {
                return reason;
            }
        }
        return null;
    }
}
