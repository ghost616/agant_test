package com.ghost616.agentbase.dto.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomToolCall {

    /** 工具调用项 ID */
    private String itemId;

    /** 输出项索引 */
    private Integer outputIndex;

    /** 工具输入（JSON 字符串） */
    private String input;

    /** 工具输出（JSON 字符串） */
    private String output;
}
