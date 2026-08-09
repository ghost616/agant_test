package com.ghost616.platform.service.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * LogData 的 Jackson mix-in，序列化时忽略 logLevel 字段。
 * 日志级别已单独存储于 agent_log.log_level 列，无需冗余写入 log_data。
 */
@JsonIgnoreProperties({"logLevel"})
public abstract class LogDataMixin {
}
