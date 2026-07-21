package com.ghost616.agentinteg.model;

public enum AnthropicModel {

    CLAUDE_SONNET_4("claude-sonnet-4-20250514"),
    CLAUDE_3_OPUS("claude-3-opus"),
    CLAUDE_3_SONNET("claude-3-sonnet"),
    CLAUDE_3_HAIKU("claude-3-haiku");

    private final String modelName;

    AnthropicModel(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }
}
