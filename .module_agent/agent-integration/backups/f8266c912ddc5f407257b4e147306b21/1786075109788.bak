package com.ghost616.agentinteg.knowledge;

import java.util.List;

/**
 * 知识库查询 Provider 接口，由外部模块提供知识库的查询能力。
 */
public interface KnowledgeBaseQueryProvider {

    /**
     * 根据会话 ID 获取关联的知识库信息。
     *
     * @param sessionId 会话 ID
     * @return 知识库基础信息
     */
    KnowledgeBaseInfo getKnowledgeBaseInfo(String sessionId);

    /**
     * 按文件名关键字搜索知识库下的文件。
     *
     * @param kbId     知识库 ID
     * @param fileName 文件名关键字（可为 null）
     * @param limit    返回数量上限
     * @return 文件信息列表
     */
    List<FileInfo> searchFiles(Long kbId, String fileName, int limit);

    /**
     * 搜索知识库文本块，返回匹配的文本块列表（含文件信息）。
     *
     * @param kbId         知识库 ID
     * @param fileId       文件 ID（可为 null，表示不限文件）
     * @param searchType   搜索类型
     * @param query        查询关键字
     * @param topK         返回数量上限
     * @param contextLines 上下文行数
     * @return 文本块列表
     */
    List<TextChunkWithFile> searchChunks(Long kbId, Long fileId, SearchType searchType, String query,
                                         int topK, int contextLines);

    /**
     * 获取指定文件中某个行号范围内的文本块。
     *
     * @param kbId      知识库 ID
     * @param fileId    文件 ID
     * @param startLine 起始行号
     * @param endLine   结束行号
     * @return 文本块
     */
    TextChunkWithFile getFileChunks(Long kbId, Long fileId, int startLine, int endLine);
}
