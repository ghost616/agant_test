package com.ghost616.platform.dto.agent;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ghost616.platform.enums.CommonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfigDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String name;
    private String description;
    private String systemPrompt;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long modelId;
    private CommonStatus status;
    private Integer recentMessageCount;
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> toolIds;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
