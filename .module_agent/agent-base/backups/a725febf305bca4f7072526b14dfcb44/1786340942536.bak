package com.ghost616.agentbase.dto.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatChunk {

    private String delta;

    private String reasoning;

    private List<ToolCallDelta> toolCalls;

    private String finishReason;

    private Boolean hasToolCalls;

    private Integer index;

    private UsageInfo usage;

    /** 响应 ID（Responses API 流式事件 response.completed 携带，供有状态续接使用） */
    private String responseId;

    /** 网络搜索调用（Responses API output item，非空时携带） */
    private WebSearchCall webSearchCall;

    /** 自定义工具调用（Responses API output item，非空时携带） */
    private CustomToolCall customToolCall;
}
