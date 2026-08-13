package com.ghost616.platform.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记忆提示语保存请求 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryPromptSaveRequest {

    /** 记忆提示语 */
    private String prompt;
}
