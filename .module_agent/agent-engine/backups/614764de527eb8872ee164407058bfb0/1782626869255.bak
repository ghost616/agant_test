package com.ghost616.platform.service.agent.invoker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.dto.model.ToolDefinition;
import com.ghost616.platform.dto.tool.ToolConfigDTO;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolDefinitionConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolDefinitionConverter() {
    }

    public static ToolDefinition createMinimal(ToolConfigDTO tool) {
        return ToolDefinition.builder()
                .name(tool.getName())
                .description(tool.getDescription())
                .build();
    }

    public static ToolDefinition convert(ToolConfigDTO tool) {
        if (tool.getParameterSchema() == null || tool.getParameterSchema().isBlank()) {
            return createMinimal(tool);
        }
        Map<String, Object> parameters;
        try {
            parameters = MAPPER.readValue(
                    tool.getParameterSchema(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            parameters = Map.of("type", "object");
        }
        if (!parameters.containsKey("type")) {
            Map<String, Object> wrapped = new LinkedHashMap<>();
            wrapped.put("type", "object");
            wrapped.put("properties", parameters);
            parameters = wrapped;
        }
        return ToolDefinition.builder()
                .name(tool.getName())
                .description(tool.getDescription())
                .parameters(parameters)
                .build();
    }
}
