package com.ghost616.agentbase.dto.model;

import com.ghost616.agentbase.enums.ModelType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModelConfigDataTest {

    @Test
    void isResponsesType对responses应返回true() {
        ModelConfigData config = new ModelConfigData(
                "1", "key", "url", "model", 0.7, 4096, "openai", "responses");
        assertTrue(config.isResponsesType());
    }

    @Test
    void isResponsesType对responses_stateless应返回true() {
        ModelConfigData config = new ModelConfigData(
                "1", "key", "url", "model", 0.7, 4096, "openai", "responses_stateless");
        assertTrue(config.isResponsesType());
    }

    @Test
    void isResponsesType对其他值应返回false() {
        ModelConfigData config = new ModelConfigData(
                "1", "key", "url", "model", 0.7, 4096, "openai", "openai");
        assertFalse(config.isResponsesType());
    }

    @Test
    void isResponsesType对null应返回false() {
        ModelConfigData config = new ModelConfigData(
                "1", "key", "url", "model", 0.7, 4096, "openai", null);
        assertFalse(config.isResponsesType());
    }

    @Test
    void 旧八参构造器应默认modelType为LLM() {
        ModelConfigData config = new ModelConfigData(
                "1", "key", "url", "model", 0.7, 4096, "openai", null);
        assertEquals(ModelType.LLM, config.modelType());
    }

    @Test
    void 全参构造器应设置modelType() {
        ModelConfigData config = new ModelConfigData(
                "1", "key", "url", "model", 0.7, 4096, "openai", ModelType.EMBEDDINGS, null);
        assertEquals(ModelType.EMBEDDINGS, config.modelType());
    }
}
