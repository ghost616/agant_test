package com.ghost616.platform.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聚合文本重生成请求 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryRegenerateRequest {

    /** 目标 ES 文档 ID（sessionId_aggregationType_startSeq_endSeq） */
    private String docId;

    /** 起始消息序号（含） */
    private Integer startSeq;

    /** 结束消息序号（含） */
    private Integer endSeq;

    /** 自定义提示语，为空时使用默认聚合提示语 */
    private String prompt;
}
