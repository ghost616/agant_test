package com.ghost616.agentbase.dto.model;

public record ModelConfigData(Long id, String apiKey, String baseUrl, String modelName,
                               Double temperature, Integer maxTokens, String platformType) {
}
