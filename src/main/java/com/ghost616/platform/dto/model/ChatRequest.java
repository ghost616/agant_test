package com.ghost616.platform.dto.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话请求 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /** 对话消息列表 */
    private List<Message> messages;

    /** 工具定义列表（可为空） */
    private List<ToolDefinition> tools;

    /** 采样温度 */
    private Double temperature;

    /** 最大生成 Token 数 */
    private Integer maxTokens;

    /** 模型标识 */
    private String model;

    /** 是否启用思考模式 */
    private Boolean thinking;
}
