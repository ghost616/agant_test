package com.ghost616.platform.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseCreateRequest {

    @NotBlank(message = "知识库名称不能为空")
    private String name;

    private String description;

    @NotNull(message = "向量模型不能为空")
    private Long vectorModelId;

    private String esIndex;
}
