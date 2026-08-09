package com.ghost616.platform.dto.agent_log;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 智能体日志数据传输对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentLogDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;

    /** 会话名（来自 session 表 title） */
    private String sessionName;

    private String conversationId;

    private String logType;

    private String logLevel;

    private String logData;

    /** 会话变量 JSON 字符串 */
    private String sessionVariables;

    /** 对话变量 JSON 字符串 */
    private String conversationVariables;

    private LocalDateTime createTime;
}
