package com.ghost616.platform.dto.model;

import com.ghost616.platform.enums.CommonStatus;
import com.ghost616.platform.enums.PlatformType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelUpdateRequest {

    private String name;

    private PlatformType platformType;

    private String apiKey;

    private String baseUrl;

    private String modelName;

    @Min(value = 0, message = "温度参数最小为0.0")
    @Max(value = 2, message = "温度参数最大为2.0")
    private Double temperature;

    @Positive(message = "最大Token数必须为正整数")
    private Integer maxTokens;

    private CommonStatus status;

    private String description;
}
