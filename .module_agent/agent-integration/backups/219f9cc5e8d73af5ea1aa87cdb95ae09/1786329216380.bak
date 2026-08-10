package com.ghost616.agentinteg.knowledge;

import java.util.List;

/**
 * 文本块数据类，包含知识库、文件信息和块列表。
 */
public record TextChunkWithFile(Long knowledgeBaseId, Long fileId, String fileName, List<TextChunk> chunkList) {

    /**
     * 单个文本块，包含行号与文本内容。
     */
    public record TextChunk(int lineNumber, String text) {
    }
}
