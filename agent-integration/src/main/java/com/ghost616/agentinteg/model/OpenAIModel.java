package com.ghost616.agentinteg.model;

public enum OpenAIModel {

    GPT_4_1("gpt-4.1"),
    GPT_4O("gpt-4o"),
    GPT_4O_MINI("gpt-4o-mini"),
    GPT_4_TURBO("gpt-4-turbo"),
    GPT_3_5_TURBO("gpt-3.5-turbo");

    private final String modelName;

    OpenAIModel(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }
}
