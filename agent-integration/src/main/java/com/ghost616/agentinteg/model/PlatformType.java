package com.ghost616.agentinteg.model;

import com.baomidou.mybatisplus.annotation.EnumValue;

import java.util.Arrays;
import java.util.List;

/**
 * LLM 平台类型枚举。
 */
public enum PlatformType {

    OPENAI("OPENAI", "OpenAI", "https://api.openai.com/v1"),
    ANTHROPIC("ANTHROPIC", "Anthropic", "https://api.anthropic.com/v1"),
    AZURE("AZURE", "Azure OpenAI", ""),
    OLLAMA("OLLAMA", "Ollama", ""),
    DEEPSEEK("DEEPSEEK", "DeepSeek", "https://api.deepseek.com"),
    KIMI("KIMI", "Kimi 月之暗面", "https://api.moonshot.cn/v1"),
    VOLCENGINE("VOLCENGINE", "火山引擎", "https://ark.cn-beijing.volces.com/api/v3"),
    CUSTOM("CUSTOM", "自定义", "");

    @EnumValue
    private final String code;
    private final String description;
    private final String defaultBaseUrl;

    PlatformType(String code, String description, String defaultBaseUrl) {
        this.code = code;
        this.description = description;
        this.defaultBaseUrl = defaultBaseUrl;
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
        return switch (this) {
            case OPENAI -> Arrays.stream(OpenAIModel.values()).map(OpenAIModel::getModelName).toList();
            case ANTHROPIC -> Arrays.stream(AnthropicModel.values()).map(AnthropicModel::getModelName).toList();
            case DEEPSEEK -> Arrays.stream(DeepSeekModel.values()).map(DeepSeekModel::getModelName).toList();
            case KIMI -> Arrays.stream(KimiModel.values()).map(KimiModel::getModelName).toList();
            case VOLCENGINE -> Arrays.stream(VolcEngineModel.values()).map(VolcEngineModel::getModelName).toList();
            case AZURE, OLLAMA, CUSTOM -> List.of();
        };
    }
}
