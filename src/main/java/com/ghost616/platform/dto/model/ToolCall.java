package com.ghost616.platform.dto.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具调用 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall {

    /** 工具调用唯一标识 */
    private String id;

    /** 工具名称 */
    private String name;

    /** 工具参数（JSON 字符串） */
    private String arguments;
}
