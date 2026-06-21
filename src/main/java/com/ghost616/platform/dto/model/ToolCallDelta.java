package com.ghost616.platform.dto.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具调用增量 DTO（流式 chunk 使用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallDelta {

    /** 工具调用唯一标识 */
    private String id;

    /** 工具名称 */
    private String name;

    /** 工具参数增量 JSON 片段（可为空） */
    private String arguments;

    /** 工具调用索引 */
    private Integer index;
}
