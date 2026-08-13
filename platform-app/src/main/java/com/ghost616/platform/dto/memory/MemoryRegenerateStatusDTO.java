package com.ghost616.platform.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聚合文本重生成状态 DTO，记录异步重生成聚合文本的执行状态与结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryRegenerateStatusDTO {

    /** 会话 ID */
    private Long sessionId;

    /** 目标 ES 文档 ID（sessionId_aggregationType_startSeq_endSeq） */
    private String docId;

    /** 执行状态：RUNNING / COMPLETED / FAILED */
    private String status;

    /** 重生成得到的聚合摘要文本（COMPLETED 时非空） */
    private String aggregationText;

    /** 失败原因（FAILED 时非空） */
    private String error;
}
