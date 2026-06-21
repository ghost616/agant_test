package com.ghost616.platform.dto.model;

import com.ghost616.platform.enums.CommonStatus;
import com.ghost616.platform.enums.PlatformType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCreateRequest {

    @NotBlank(message = "模型名称不能为空")
    private String name;

    @NotNull(message = "平台类型不能为空")
    private PlatformType platformType;

    @NotBlank(message = "API密钥不能为空")
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
