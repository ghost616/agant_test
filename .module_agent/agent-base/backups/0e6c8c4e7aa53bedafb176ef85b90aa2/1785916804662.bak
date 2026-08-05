package com.ghost616.agentbase.dto.model;

import com.ghost616.agentbase.enums.RequestType;

public record ModelConfigData(String id, String apiKey, String baseUrl, String modelName,
                               Double temperature, Integer maxTokens, String platformType,
                               String requestType) {

    public boolean isResponsesType() {
        return RequestType.isResponses(requestType);
    }
}
