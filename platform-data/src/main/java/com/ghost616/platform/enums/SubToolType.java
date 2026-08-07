package com.ghost616.platform.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum SubToolType {

    BROWSER("BROWSER", "浏览器"),
    RAG_KNOWLEDGE("RAG_KNOWLEDGE", "知识库检索");

    @EnumValue
    private final String code;
    private final String description;

    SubToolType(String code, String description) {
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
