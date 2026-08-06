package com.ghost616.platform.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识库文本块模型，对应 Elasticsearch 索引中的文档结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long knowledgeBaseId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    private Integer lineNumber;

    private List<Float> vector;

    private String text;

    private Boolean kbEnabled;

    private Boolean fileEnabled;
}
