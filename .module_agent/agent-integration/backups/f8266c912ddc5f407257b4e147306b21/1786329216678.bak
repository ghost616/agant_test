package com.ghost616.agentinteg.knowledge;

import java.util.List;

/**
 * 知识库查询 Provider 接口，由外部模块提供知识库的查询能力。
 */
public interface KnowledgeBaseQueryProvider {

    /**
     * 根据会话 ID 获取关联的知识库信息列表。
     *
     * @param sessionId 会话 ID
     * @return 知识库基础信息列表，会话或知识库不存在时返回空列表
     */
    List<KnowledgeBaseInfo> getKnowledgeBaseInfo(String sessionId);

    /**
     * 按文件名关键字搜索知识库下的文件，仅返回已发布到 ES 的文件。
     *
     * @param kbId     知识库 ID
     * @param fileName 文件名关键字（可为 null）
     * @param limit    返回数量上限
     * @return 文件信息列表（仅已发布到 ES 的文件）
     */
    List<FileInfo> searchFiles(Long kbId, String fileName, int limit);

    /**
     * 搜索知识库文本块，返回匹配的文本块列表（含文件信息）。
     * 参数 fileId 作为 ES 查询的过滤条件在查询层面生效（非内存过滤），非 null 时仅返回该文件下的文本块。
     *
     * @param kbId       知识库 ID
     * @param fileId     文件 ID（可为 null，表示不限文件；非 null 时在 ES 查询中过滤）
     * @param searchType 搜索类型
     * @param query      查询关键字
     * @param topK       返回数量上限
     * @return 文本块列表
     */
    List<TextChunkWithFile> searchChunks(Long kbId, Long fileId, SearchType searchType, String query, int topK);

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
