package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ghost616.agentbase.enums.CommonStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库实体，映射 knowledge_base 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_base")
public class KnowledgeBase extends BaseEntity {

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("status")
    private CommonStatus status;

    @TableField("vector_model_id")
    private Long vectorModelId;

    @TableField("es_index")
    private String esIndex;

    @TableField("rebuilding")
    private Boolean rebuilding = false;
}
