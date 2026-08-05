package com.ghost616.agentbase.dto.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量化请求 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequest {

    /** 模型标识 */
    private String model;

    /** 待向量化的单个文本 */
    private String input;

    /** 待向量化的文本列表 */
    private List<String> inputList;
}
