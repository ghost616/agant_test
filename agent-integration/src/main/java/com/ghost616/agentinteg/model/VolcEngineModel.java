package com.ghost616.agentinteg.model;

public enum VolcEngineModel {

    SEED_EVOLVING("doubao-seed-evolving"),
    SEED_2_1_TURBO("doubao-seed-2-1-turbo-260628"),
    SEED_2_1_PRO("doubao-seed-2-1-pro-260628");

    private final String modelName;

    VolcEngineModel(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }
}
