package com.ghost616.agentbase.dto.model;

import com.ghost616.agentbase.enums.ModelType;
import com.ghost616.agentbase.enums.RequestType;

public record ModelConfigData(String id, String apiKey, String baseUrl, String modelName,
                               Double temperature, Integer maxTokens, String platformType,
                               ModelType modelType, String requestType) {

    public ModelConfigData(String id, String apiKey, String baseUrl, String modelName,
                           Double temperature, Integer maxTokens, String platformType,
                           String requestType) {
        this(id, apiKey, baseUrl, modelName, temperature, maxTokens, platformType,
                ModelType.LLM, requestType);
    }

    public boolean isResponsesType() {
        return RequestType.isResponses(requestType);
    }
}
