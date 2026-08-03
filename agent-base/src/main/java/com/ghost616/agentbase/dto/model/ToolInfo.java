package com.ghost616.agentbase.dto.model;

/**
 * 工具调用信息（tool 角色消息回传用），含调用 ID 与工具名。
 */
public record ToolInfo(String toolCallId, String toolName) {
}
