package com.ghost616.platform.service.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * SessionLogData 的 Jackson mix-in，序列化时忽略 logLevel、sessionId 与 conversationId 字段。
 * 会话 ID/对话 ID 已单独提取存储于 agent_log 表对应列，无需冗余写入 log_data；
 * 日志级别已单独存储于 agent_log.log_level 列。
 */
@JsonIgnoreProperties({"logLevel", "sessionId", "conversationId"})
public abstract class SessionLogDataMixin {
}
