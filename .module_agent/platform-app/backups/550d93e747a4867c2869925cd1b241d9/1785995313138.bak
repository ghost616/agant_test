package com.ghost616.platform.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
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
}
