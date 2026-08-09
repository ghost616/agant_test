package com.ghost616.platform.service.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * ContextLogData 的 Jackson mix-in，序列化时忽略 logLevel 与 context 字段。
 * 执行上下文信息冗余且体积较大，会话 ID/对话 ID 已单独提取存储；日志级别已单独存储于 agent_log.log_level 列。
 * 注意：Jackson 的 @JsonIgnoreProperties 在类层次上取最近派生注解且不跨层合并，
 * 因此此处需同时包含父类 LogDataMixin 排除的 logLevel，避免覆盖父类排除规则。
 */
@JsonIgnoreProperties({"logLevel", "context"})
public abstract class ContextLogDataMixin {
}
