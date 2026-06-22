package com.ghost616.platform.dto.model;

import com.ghost616.platform.enums.CommonStatus;
import com.ghost616.platform.enums.PlatformType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String name;
    private PlatformType platformType;
    private String apiKey;
    private String baseUrl;
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private CommonStatus status;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
