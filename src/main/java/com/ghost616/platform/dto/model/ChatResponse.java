package com.ghost616.platform.dto.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话响应 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /** 模型回复内容 */
    private String content;

    /** 工具调用列表（可为空） */
    private List<ToolCall> toolCalls;

    /** Token 用量信息 */
    private UsageInfo usage;

    /** 结束原因（stop/length/tool_calls 等） */
    private String finishReason;
}
