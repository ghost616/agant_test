package com.ghost616.platform.dto.knowledge;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ghost616.agentbase.enums.CommonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeFileDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String fileName;
    private String fileDescription;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long knowledgeBaseId;
    private Long fileSize;
    private Integer lineCount;
    private CommonStatus status;
    private String fileContent;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
