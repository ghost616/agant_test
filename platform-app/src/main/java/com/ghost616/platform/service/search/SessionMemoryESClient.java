package com.ghost616.platform.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.ghost616.platform.dto.PageResult;
import com.ghost616.platform.enums.AggregationType;
import com.ghost616.platform.model.SessionMemoryDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 会话记忆客户端，封装 Elasticsearch 索引 session_memory 的创建与会话记忆文档批量写入。
 */
@Service
@RequiredArgsConstructor
public class SessionMemoryESClient {

    /** 会话记忆索引名 */
    public static final String INDEX_NAME = "session_memory";

    private final ElasticsearchClient elasticsearchClient;

    /**
     * 检查索引是否存在。
     *
     * @return 索引存在返回 true
     */
    public boolean indexExists() {
        try {
            return elasticsearchClient.indices().exists(e -> e.index(INDEX_NAME)).value();
        } catch (IOException e) {
            throw new IllegalStateException("检查索引是否存在失败: " + INDEX_NAME, e);
        }
    }

    /**
     * 创建索引 session_memory，mapping 包含 sessionId/keyword、聚合类型/keyword、聚合序号/integer、聚合文本/text(ik)、向量/dense_vector(cosine)。
     */
    public void createIndex() {
        try {
            if (indexExists()) {
                return;
            }
            elasticsearchClient.indices().create(c -> c
                    .index(INDEX_NAME)
                    .mappings(m -> m
                            .properties("sessionId", p -> p.keyword(k -> k))
                            .properties("aggregationType", p -> p.keyword(k -> k))
                            .properties("aggregationStartSeq", p -> p.integer(i -> i))
                            .properties("aggregationEndSeq", p -> p.integer(i -> i))
                            .properties("aggregationStartTime", p -> p.long_(l -> l))
                            .properties("aggregationEndTime", p -> p.long_(l -> l))
                            .properties("aggregationText", p -> p.text(t -> t
                                    .analyzer("ik_max_word")
                                    .searchAnalyzer("ik_smart")))
                            .properties("vector", p -> p.denseVector(d -> d
                                    .similarity(DenseVectorSimilarity.Cosine)))));
        } catch (IOException e) {
            throw new IllegalStateException("创建索引失败: " + INDEX_NAME, e);
        }
    }

    /**
     * 批量保存会话记忆文档到索引，索引不存在时自动创建。
     *
     * @param documents 会话记忆文档列表
     */
    public void batchSave(List<SessionMemoryDocument> documents) {
        ensureIndex();
        if (documents == null || documents.isEmpty()) {
            return;
        }
        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (SessionMemoryDocument document : documents) {
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(INDEX_NAME)
                                .id(buildDocId(document))
                                .document(document)));
            }
            elasticsearchClient.bulk(bulkBuilder.build());
        } catch (IOException e) {
            throw new IllegalStateException("批量保存会话记忆文档失败: " + INDEX_NAME, e);
        }
    }

    /**
     * 检查指定会话在给定时间段内是否已存在按日聚合（DAILY）文档。
     * 重叠判断采用半开区间：checkStartTime &lt; 文档 aggregationEndTime 且 checkEndTime &gt; 文档 aggregationStartTime，
     * 即文档 startTime &lt; checkEndTime 且文档 endTime &gt; checkStartTime，避免相邻自然日边界重叠误判。
     *
     * @param sessionId 会话 ID（字符串形式）
     * @param startTime 时间段起始时间戳（半开区间起点，不含）
     * @param endTime   时间段结束时间戳（半开区间终点，不含）
     * @return 已存在 DAILY 文档返回 true
     */
    public boolean checkDailyExists(String sessionId, long startTime, long endTime) {
        ensureIndex();
        try {
            SearchResponse<SessionMemoryDocument> response = elasticsearchClient.search(s -> s
                            .index(INDEX_NAME)
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.term(t -> t.field("sessionId").value(sessionId)))
                                    .must(m -> m.term(t -> t.field("aggregationType").value(AggregationType.DAILY.getCode())))
                                    .must(m -> m.range(r -> r
                                            .number(n -> n
                                                    .field("aggregationStartTime")
                                                    .lt((double) endTime))))
                                    .must(m -> m.range(r -> r
                                            .number(n -> n
                                                    .field("aggregationEndTime")
                                                    .gt((double) startTime))))))
                            .size(1),
                    SessionMemoryDocument.class);
            List<Hit<SessionMemoryDocument>> hits = response.hits().hits();
            return hits != null && !hits.isEmpty();
        } catch (IOException e) {
            throw new IllegalStateException("检查按日记忆文档是否存在失败: " + INDEX_NAME, e);
        }
    }

    /**
     * 按会话 ID 与聚合类型查询会话记忆文档，按聚合开始时间降序排列并分页返回。
     *
     * @param sessionId 会话 ID（字符串形式）
     * @param type      聚合类型（GROUP/DAILY）
     * @param page      页码（从 1 开始）
     * @param size      每页条数
     * @return 分页结果，包含文档列表与总数
     */
    public PageResult<SessionMemoryDocument> queryBySessionId(String sessionId, AggregationType type, int page, int size) {
        ensureIndex();
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        try {
            SearchResponse<SessionMemoryDocument> response = elasticsearchClient.search(s -> s
                            .index(INDEX_NAME)
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.term(t -> t.field("sessionId").value(sessionId)))
                                    .must(m -> m.term(t -> t.field("aggregationType").value(type.getCode())))))
                            .sort(srt -> srt.field(f -> f.field("aggregationStartTime").order(SortOrder.Desc)))
                            .from((safePage - 1) * safeSize)
                            .size(safeSize),
                    SessionMemoryDocument.class);
            List<Hit<SessionMemoryDocument>> hits = response.hits().hits();
            List<SessionMemoryDocument> documents = hits == null ? List.of()
                    : hits.stream()
                            .map(Hit::source)
                            .filter(Objects::nonNull)
                            .toList();
            long total = response.hits().total() != null ? response.hits().total().value() : 0L;
            return new PageResult<>(documents, total, safePage, safeSize);
        } catch (IOException e) {
            throw new IllegalStateException("按会话查询记忆文档失败: " + INDEX_NAME, e);
        }
    }

    /**
     * 按文档 ID 更新现有会话记忆文档，仅更新 aggregationText 与 vector 字段，不新增文档。
     *
     * @param docId  文档 ID（sessionId_aggregationType_startSeq_endSeq）
     * @param text   新的聚合摘要文本
     * @param vector 新的摘要向量
     */
    public void updateDocument(String docId, String text, List<Float> vector) {
        ensureIndex();
        Map<String, Object> partial = new HashMap<>();
        partial.put("aggregationText", text);
        partial.put("vector", vector);
        try {
            elasticsearchClient.update(u -> u
                            .index(INDEX_NAME)
                            .id(docId)
                            .doc(JsonData.of(partial)),
                    SessionMemoryDocument.class);
        } catch (IOException e) {
            throw new IllegalStateException("更新会话记忆文档失败: " + INDEX_NAME, e);
        }
    }

    private void ensureIndex() {
        if (!indexExists()) {
            createIndex();
        }
    }

    private String buildDocId(SessionMemoryDocument document) {
        return document.getSessionId() + "_" + document.getAggregationType() + "_"
                + document.getAggregationStartSeq() + "_" + document.getAggregationEndSeq();
    }
}
