package com.ghost616.agentinteg.model;

public enum KimiModel {

    K2_7_CODE("kimi-k2.7-code"),
    K2_6("kimi-k2.6"),
    K2_5("kimi-k2.5"),
    K3("kimi-k3");

    private final String modelName;

    KimiModel(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }
}
