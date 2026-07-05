package com.ghost616.agentbase.dto.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具定义 DTO（JSON Schema 格式）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {

    /** 工具名称 */
    private String name;

    /** 工具描述 */
    private String description;

    /** 工具参数定义（JSON Schema） */
    private Map<String, Object> parameters;
}
