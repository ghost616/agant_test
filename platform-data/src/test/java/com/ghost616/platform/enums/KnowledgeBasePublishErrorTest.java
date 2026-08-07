package com.ghost616.platform.enums;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeBasePublishErrorTest {

    @Test
    void enumCountAndCodes() {
        KnowledgeBasePublishError[] values = KnowledgeBasePublishError.values();
        assertEquals(5, values.length);
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("EMPTY_CONTENT", "文件内容为空不能发布");
        expected.put("VECTOR_MODEL_NOT_CONFIGURED", "向量模型未配置");
        expected.put("KNOWLEDGE_BASE_REBUILDING", "知识库正在ES数据重构中");
        expected.put("FILE_PUBLISHING", "文件正在发布中不能修改内容");
        expected.put("FILE_PUBLISH_FAILED", "文件发布失败");
        assertEquals(expected.size(), values.length);
        for (KnowledgeBasePublishError v : values) {
            assertEquals(expected.get(v.name()), v.getMessage(), "message mismatch for " + v.name());
        }
    }

    @Test
    void codeFormatFollowsKnowledgePublishPattern() {
        KnowledgeBasePublishError[] values = KnowledgeBasePublishError.values();
        for (int i = 0; i < values.length; i++) {
            String expectedCode = String.format("KNOWLEDGE-PUBLISH-%03d", i + 1);
            assertEquals(expectedCode, values[i].getCode(), "code mismatch for " + values[i].name());
            assertTrue(values[i].getCode().startsWith("KNOWLEDGE-PUBLISH-"), "code should follow KNOWLEDGE-PUBLISH-XXX format");
        }
    }
}
