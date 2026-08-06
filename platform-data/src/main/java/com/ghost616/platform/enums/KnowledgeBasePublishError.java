package com.ghost616.platform.enums;

/**
 * 知识库发布错误码枚举。
 */
public enum KnowledgeBasePublishError {

    EMPTY_CONTENT("KNOWLEDGE-PUBLISH-001", "文件内容为空不能发布"),
    VECTOR_MODEL_NOT_CONFIGURED("KNOWLEDGE-PUBLISH-002", "向量模型未配置"),
    KNOWLEDGE_BASE_REBUILDING("KNOWLEDGE-PUBLISH-003", "知识库正在ES数据重构中"),
    FILE_PUBLISHING("KNOWLEDGE-PUBLISH-004", "文件正在发布中不能修改内容"),
    FILE_PUBLISH_FAILED("KNOWLEDGE-PUBLISH-005", "文件发布失败");

    private final String code;
    private final String message;

    KnowledgeBasePublishError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
