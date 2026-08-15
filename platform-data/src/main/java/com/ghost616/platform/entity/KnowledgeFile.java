package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.enums.PublishStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文件实体，映射 knowledge_file 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_file")
public class KnowledgeFile extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_description")
    private String fileDescription;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("file_size")
    private Long fileSize;

    @TableField("line_count")
    private Integer lineCount;

    @TableField("status")
    private CommonStatus status;

    @TableField("publish_status")
    private PublishStatus publishStatus;

    @TableField("file_content")
    private String fileContent;
}
