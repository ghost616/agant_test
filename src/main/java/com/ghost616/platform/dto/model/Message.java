package com.ghost616.platform.dto.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话消息 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    /** 消息角色（system/user/assistant/tool） */
    private String role;

    /** 消息内容 */
    private String content;

    /** 工具调用列表（assistant 消息中的 tool_calls） */
    private List<ToolCall> toolCalls;

    /** 工具调用 ID（tool 角色消息回传用） */
    private String toolCallId;
}
