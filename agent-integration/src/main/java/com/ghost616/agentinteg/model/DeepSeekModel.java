package com.ghost616.agentinteg.model;

public enum DeepSeekModel {

    V4_FLASH("deepseek-v4-flash"),
    V4_PRO("deepseek-v4-pro");

    private final String modelName;

    DeepSeekModel(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }
}
