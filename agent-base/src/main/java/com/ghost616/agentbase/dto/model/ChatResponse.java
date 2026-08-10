package com.ghost616.agentbase.dto.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.ghost616.agentbase.enums.FinishReason;

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
    private FinishReason finishReason;

    /** 响应 ID（Responses API 返回值，供下一轮续接使用） */
    private String responseId;
}
