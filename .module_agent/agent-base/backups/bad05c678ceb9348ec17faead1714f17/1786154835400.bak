package com.ghost616.agentbase.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "sessionId不能为空")
    private String sessionId;

    @NotBlank(message = "content不能为空")
    private String content;

    private String modelId;

    private Boolean thinking;

    /** 上一轮响应的 ID（Responses API 多轮续接时透传给模型请求） */
    private String previousResponseId;
}
