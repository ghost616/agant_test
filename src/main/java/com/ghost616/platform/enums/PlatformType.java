package com.ghost616.platform.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import java.util.List;

/**
 * LLM 平台类型枚举。
 */
public enum PlatformType {

    OPENAI("OPENAI", "OpenAI", "https://api.openai.com/v1",
            List.of("gpt-4.1", "gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-3.5-turbo")),
    ANTHROPIC("ANTHROPIC", "Anthropic", "https://api.anthropic.com/v1",
            List.of("claude-sonnet-4-20250514", "claude-3-opus", "claude-3-sonnet", "claude-3-haiku")),
    AZURE("AZURE", "Azure OpenAI", "", List.of()),
    OLLAMA("OLLAMA", "Ollama", "", List.of()),
    DEEPSEEK("DEEPSEEK", "DeepSeek", "https://api.deepseek.com",
            List.of("deepseek-v4-flash", "deepseek-v4-pro")),
    CUSTOM("CUSTOM", "自定义", "", List.of());

    @EnumValue
    private final String code;
    private final String description;
    private final String defaultBaseUrl;
    private final List<String> defaultModelNames;

    PlatformType(String code, String description, String defaultBaseUrl, List<String> defaultModelNames) {
        this.code = code;
        this.description = description;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModelNames = defaultModelNames;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    public List<String> getDefaultModelNames() {
        return defaultModelNames;
    }
}
