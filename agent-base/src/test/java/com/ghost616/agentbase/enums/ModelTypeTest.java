package com.ghost616.agentbase.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModelTypeTest {

    @Test
    void 枚举值应存在() {
        assertNotNull(ModelType.LLM);
        assertNotNull(ModelType.EMBEDDINGS);
    }

    @Test
    void LLM的getCode应返回LLM() {
        assertEquals("LLM", ModelType.LLM.getCode());
    }

    @Test
    void LLM的getDescription应返回大语言模型() {
        assertEquals("大语言模型", ModelType.LLM.getDescription());
    }

    @Test
    void EMBEDDINGS的getCode应返回EMBEDDINGS() {
        assertEquals("EMBEDDINGS", ModelType.EMBEDDINGS.getCode());
    }

    @Test
    void EMBEDDINGS的getDescription应返回向量嵌入模型() {
        assertEquals("向量嵌入模型", ModelType.EMBEDDINGS.getDescription());
    }
}
