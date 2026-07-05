package com.ghost616.agentbase.dto.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 用量信息 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageInfo {

    /** 提示词 Token 数 */
    private Integer promptTokens;

    /** 补全 Token 数 */
    private Integer completionTokens;

    /** 总 Token 数 */
    private Integer totalTokens;
}
