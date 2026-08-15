package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ModelType;
import com.ghost616.agentinteg.model.PlatformType;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName("model_config")
public class ModelConfig extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    private String name;

    private PlatformType platformType;

    private String apiKey;

    private String baseUrl;

    private String modelName;

    @TableField("request_type")
    private String requestType;

    @TableField("model_type")
    private ModelType modelType = ModelType.LLM;

    private Double temperature;

    private Integer maxTokens;

    private CommonStatus status;

    private String description;
}
