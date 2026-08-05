package com.ghost616.agentbase.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 模型类型枚举，定义模型的能力类型。
 */
public enum ModelType {

    LLM("LLM", "大语言模型"),
    EMBEDDINGS("EMBEDDINGS", "向量嵌入模型");

    @EnumValue
    private final String code;
    private final String description;

    ModelType(String code, String description) {
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
