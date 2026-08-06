package com.ghost616.platform.dto.knowledge;

import com.ghost616.agentbase.enums.CommonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseUpdateRequest {

    private String name;

    private String description;

    private CommonStatus status;

    private Long vectorModelId;

    private String esIndex;
}
