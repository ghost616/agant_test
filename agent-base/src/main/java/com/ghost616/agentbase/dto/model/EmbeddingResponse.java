package com.ghost616.agentbase.dto.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量化响应 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResponse {

    /** 向量列表，每项包含索引与浮点数组 */
    private List<EmbeddingItem> embeddings;

    /** Token 用量信息 */
    private UsageInfo usage;

    /**
     * 单个向量项。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddingItem {

        /** 向量索引 */
        private Integer index;

        /** 向量浮点数组 */
        private List<Float> embedding;
    }
}
