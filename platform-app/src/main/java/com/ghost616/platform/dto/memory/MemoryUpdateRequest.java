package com.ghost616.platform.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聚合文本保存（重新向量化 + 更新 ES）请求 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryUpdateRequest {

    /** 目标 ES 文档 ID（sessionId_aggregationType_startSeq_endSeq） */
    private String docId;

    /** 新的聚合摘要文本 */
    private String text;
}
