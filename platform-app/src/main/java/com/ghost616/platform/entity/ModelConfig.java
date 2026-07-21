package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ghost616.agentinteg.model.PlatformType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.ghost616.agentbase.enums.CommonStatus;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName("model_config")
public class ModelConfig extends BaseEntity {

    private String name;

    private PlatformType platformType;

    private String apiKey;

    private String baseUrl;

    private String modelName;

    private Double temperature;

    private Integer maxTokens;

    private CommonStatus status;

    private String description;
}
