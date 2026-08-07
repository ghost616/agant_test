package com.ghost616.platform.service.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.ghost616.agentbase.dto.model.EmbeddingRequest;
import com.ghost616.agentbase.dto.model.EmbeddingResponse;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import com.ghost616.agentinteg.knowledge.FileInfo;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseInfo;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseQueryProvider;
import com.ghost616.agentinteg.knowledge.SearchType;
import com.ghost616.agentinteg.knowledge.TextChunkWithFile;
import com.ghost616.platform.entity.AgentKnowledgeBase;
import com.ghost616.platform.entity.KnowledgeBase;
import com.ghost616.platform.entity.KnowledgeFile;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.model.TextChunk;
import com.ghost616.platform.repository.AgentKnowledgeBaseMapper;
import com.ghost616.platform.repository.KnowledgeBaseMapper;
import com.ghost616.platform.repository.KnowledgeFileMapper;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.service.search.KnowledgeSearchClient;
import com.ghost616.platform.util.IdConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库查询 Provider 实现，基于 platform-app 的持久化组件提供知识库、文件与文本块的查询能力。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseQueryProviderImpl implements KnowledgeBaseQueryProvider {

    private final SessionMapper sessionMapper;
    private final AgentKnowledgeBaseMapper agentKnowledgeBaseMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeFileMapper knowledgeFileMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final ModelInvokerManager modelInvokerManager;
    private final KnowledgeSearchClient knowledgeSearchClient;

    /**
     * 根据会话 ID 获取关联的知识库信息。
     *
     * @param sessionId 会话 ID
     * @return 知识库基础信息，会话或知识库不存在时返回 null
     */
    @Override
    public KnowledgeBaseInfo getKnowledgeBaseInfo(String sessionId) {
        Long sessionIdLong = IdConverter.parse(sessionId);
        if (sessionIdLong == null) {
            return null;
        }
        Session session = sessionMapper.selectById(sessionIdLong);
        if (session == null || session.getAgentId() == null) {
            return null;
        }
        List<AgentKnowledgeBase> bindings = agentKnowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<AgentKnowledgeBase>()
                        .eq(AgentKnowledgeBase::getAgentId, session.getAgentId()));
        if (bindings.isEmpty()) {
            return null;
        }
        // 当前业务仅支持单个知识库绑定，多绑定时取第一条记录
        if (bindings.size() > 1) {
            log.warn("智能体 {} 绑定了多个知识库（{} 条），仅取第一个 kbId={}",
                    session.getAgentId(), bindings.size(), bindings.get(0).getKnowledgeBaseId());
        }
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(bindings.get(0).getKnowledgeBaseId());
        if (knowledgeBase == null) {
            return null;
        }
        return new KnowledgeBaseInfo(knowledgeBase.getId(), knowledgeBase.getName(), knowledgeBase.getDescription());
    }

    /**
     * 按文件名关键字搜索知识库下的文件。
     *
     * @param kbId     知识库 ID
     * @param fileName 文件名关键字（可为 null）
     * @param limit    返回数量上限
     * @return 文件信息列表
     */
    @Override
    public List<FileInfo> searchFiles(Long kbId, String fileName, int limit) {
        LambdaQueryWrapper<KnowledgeFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeFile::getKnowledgeBaseId, kbId);
        if (StringUtils.isNotBlank(fileName)) {
            wrapper.like(KnowledgeFile::getFileName, fileName);
        }
        wrapper.orderByDesc(KnowledgeFile::getCreateTime);
        int effectiveLimit = Math.max(limit, 0);
        if (effectiveLimit == 0) {
            return List.of();
        }
        wrapper.last("LIMIT " + effectiveLimit);
        return knowledgeFileMapper.selectList(wrapper).stream()
                .map(file -> new FileInfo(file.getId(), file.getFileName(), file.getFileDescription(),
                        computeLineCount(file.getFileContent())))
                .toList();
    }

    /**
     * 搜索知识库文本块，返回匹配的文本块列表（含文件信息）。
     *
     * @param kbId       知识库 ID
     * @param fileId     文件 ID（可为 null，表示不限文件）
     * @param searchType 搜索类型
     * @param query      查询关键字
     * @param topK       返回数量上限
     * @return 文本块列表
     */
    @Override
    public List<TextChunkWithFile> searchChunks(Long kbId, Long fileId, SearchType searchType, String query,
                                                int topK) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(kbId);
        if (knowledgeBase == null || StringUtils.isBlank(knowledgeBase.getEsIndex())) {
            return List.of();
        }
        String indexName = knowledgeBase.getEsIndex();
        List<TextChunk> matched = search(indexName, kbId, knowledgeBase, searchType, query, topK);
        if (matched.isEmpty()) {
            return List.of();
        }
        if (fileId != null) {
            matched.removeIf(chunk -> !fileId.equals(chunk.getFileId()));
        }
        if (matched.isEmpty()) {
            return List.of();
        }
        Map<Long, List<TextChunk>> chunksByFile = new LinkedHashMap<>();
        for (TextChunk chunk : matched) {
            chunksByFile.computeIfAbsent(chunk.getFileId(), k -> new ArrayList<>()).add(chunk);
        }
        List<TextChunkWithFile> results = new ArrayList<>();
        for (Map.Entry<Long, List<TextChunk>> entry : chunksByFile.entrySet()) {
            Long fileIdKey = entry.getKey();
            KnowledgeFile file = knowledgeFileMapper.selectById(fileIdKey);
            String fileName = file != null ? file.getFileName() : String.valueOf(fileIdKey);
            // 按文件分组返回命中文本块，并按 lineNumber 去重
            Map<Integer, TextChunkWithFile.TextChunk> dedup = new LinkedHashMap<>();
            for (TextChunk chunk : entry.getValue()) {
                dedup.putIfAbsent(chunk.getLineNumber(),
                        new TextChunkWithFile.TextChunk(chunk.getLineNumber(), chunk.getText()));
            }
            results.add(new TextChunkWithFile(kbId, fileIdKey, fileName, new ArrayList<>(dedup.values())));
        }
        return results;
    }

    /**
     * 获取指定文件中某个行号范围内的文本块。
     *
     * @param kbId      知识库 ID
     * @param fileId    文件 ID
     * @param startLine 起始行号
     * @param endLine   结束行号
     * @return 文本块，知识库或索引缺失时返回 null
     */
    @Override
    public TextChunkWithFile getFileChunks(Long kbId, Long fileId, int startLine, int endLine) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(kbId);
        if (knowledgeBase == null || StringUtils.isBlank(knowledgeBase.getEsIndex())) {
            return null;
        }
        KnowledgeFile file = knowledgeFileMapper.selectById(fileId);
        String fileName = file != null ? file.getFileName() : String.valueOf(fileId);
        List<TextChunk> chunks = knowledgeSearchClient.searchByFileAndLineRange(
                knowledgeBase.getEsIndex(), kbId, fileId, startLine, endLine);
        List<TextChunkWithFile.TextChunk> chunkList = chunks.stream()
                .map(chunk -> new TextChunkWithFile.TextChunk(chunk.getLineNumber(), chunk.getText()))
                .toList();
        return new TextChunkWithFile(kbId, fileId, fileName, chunkList);
    }

    private List<TextChunk> search(String indexName, Long kbId, KnowledgeBase knowledgeBase,
                                   SearchType searchType, String query, int topK) {
        switch (searchType) {
            case VECTOR:
                return knowledgeSearchClient.vectorSearch(indexName, kbId, embedQuery(knowledgeBase, query), topK);
            case FULLTEXT:
                return knowledgeSearchClient.fullTextSearch(indexName, kbId, query, topK);
            case HYBRID:
                Map<String, TextChunk> dedup = new LinkedHashMap<>();
                for (TextChunk chunk : knowledgeSearchClient.vectorSearch(
                        indexName, kbId, embedQuery(knowledgeBase, query), topK)) {
                    dedup.putIfAbsent(docKey(chunk), chunk);
                }
                for (TextChunk chunk : knowledgeSearchClient.fullTextSearch(indexName, kbId, query, topK)) {
                    dedup.putIfAbsent(docKey(chunk), chunk);
                }
                return new ArrayList<>(dedup.values());
            default:
                return List.of();
        }
    }

    private String docKey(TextChunk chunk) {
        return chunk.getKnowledgeBaseId() + "_" + chunk.getFileId() + "_" + chunk.getLineNumber();
    }

    private List<Float> embedQuery(KnowledgeBase knowledgeBase, String query) {
        Long vectorModelId = knowledgeBase.getVectorModelId();
        if (vectorModelId == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "知识库未配置向量模型");
        }
        ModelConfig config = modelConfigMapper.selectById(vectorModelId);
        if (config == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        ModelInvoker invoker = modelInvokerManager.getInvoker(buildModelConfigData(config));
        EmbeddingResponse response = invoker.embed(EmbeddingRequest.builder()
                .model(config.getModelName())
                .inputList(List.of(query))
                .build());
        if (response == null || response.getEmbeddings() == null || response.getEmbeddings().isEmpty()) {
            return List.of();
        }
        EmbeddingResponse.EmbeddingItem item = response.getEmbeddings().get(0);
        return item == null || item.getEmbedding() == null ? List.of() : item.getEmbedding();
    }

    private ModelConfigData buildModelConfigData(ModelConfig config) {
        return new ModelConfigData(
                IdConverter.toString(config.getId()),
                config.getApiKey(),
                config.getBaseUrl(),
                config.getModelName(),
                config.getTemperature(),
                config.getMaxTokens(),
                config.getPlatformType() != null ? config.getPlatformType().name() : null,
                config.getRequestType()
        );
    }

    private int computeLineCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.split("\n", -1).length;
    }
}
