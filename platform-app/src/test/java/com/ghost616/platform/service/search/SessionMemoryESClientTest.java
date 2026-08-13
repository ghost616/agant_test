package com.ghost616.platform.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorProperty;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.util.ObjectBuilder;
import com.ghost616.platform.dto.PageResult;
import com.ghost616.platform.enums.AggregationType;
import com.ghost616.platform.model.SessionMemoryDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionMemoryESClientTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private ElasticsearchIndicesClient indicesClient;

    private SessionMemoryESClient client;

    @BeforeEach
    void setUp() {
        lenient().when(elasticsearchClient.indices()).thenReturn(indicesClient);
        client = new SessionMemoryESClient(elasticsearchClient);
    }

    @SuppressWarnings("unchecked")
    private <T, B> T apply(Function<B, ObjectBuilder<T>> fn, B builder) {
        return fn.apply(builder).build();
    }

    private void stubExists(boolean exists) throws Exception {
        BooleanResponse response = mock(BooleanResponse.class);
        when(response.value()).thenReturn(exists);
        when(indicesClient.exists(any(Function.class))).thenReturn(response);
    }

    @Test
    @DisplayName("indexExists：返回布尔值")
    void indexExists() throws Exception {
        BooleanResponse response = mock(BooleanResponse.class);
        when(response.value()).thenReturn(true);
        when(indicesClient.exists(any(Function.class))).thenReturn(response);

        assertTrue(client.indexExists());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>> captor =
                ArgumentCaptor.forClass(Function.class);
        verify(indicesClient).exists(captor.capture());
        ExistsRequest request = apply(captor.getValue(), new ExistsRequest.Builder());
        assertEquals(List.of(SessionMemoryESClient.INDEX_NAME), request.index());
    }

    @Test
    @DisplayName("createIndex：索引不存在时创建，mapping 含 8 字段")
    void createIndex() throws Exception {
        stubExists(false);
        when(indicesClient.create(any(Function.class))).thenReturn(null);

        client.createIndex();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>>> captor =
                ArgumentCaptor.forClass(Function.class);
        verify(indicesClient).create(captor.capture());
        CreateIndexRequest request = apply(captor.getValue(), new CreateIndexRequest.Builder());
        assertEquals(SessionMemoryESClient.INDEX_NAME, request.index());

        Map<String, Property> properties = request.mappings().properties();
        assertEquals(8, properties.size());

        Property vector = properties.get("vector");
        assertTrue(vector.isDenseVector(), "vector 应为 dense_vector");
        DenseVectorProperty dv = vector.denseVector();
        assertEquals(DenseVectorSimilarity.Cosine, dv.similarity());

        Property text = properties.get("aggregationText");
        assertTrue(text.isText(), "aggregationText 应为 text 类型");
        TextProperty tp = text.text();
        assertEquals("ik_max_word", tp.analyzer());
        assertEquals("ik_smart", tp.searchAnalyzer());

        assertTrue(properties.get("sessionId").isKeyword(), "sessionId 应为 keyword");
        assertTrue(properties.get("aggregationType").isKeyword(), "aggregationType 应为 keyword");
        assertTrue(properties.get("aggregationStartSeq").isInteger(), "aggregationStartSeq 应为 integer");
        assertTrue(properties.get("aggregationEndSeq").isInteger(), "aggregationEndSeq 应为 integer");
        assertTrue(properties.get("aggregationStartTime").isLong(), "aggregationStartTime 应为 long");
        assertTrue(properties.get("aggregationEndTime").isLong(), "aggregationEndTime 应为 long");
    }

    @Test
    @DisplayName("createIndex：索引已存在时跳过创建")
    void createIndex_alreadyExists_skip() throws Exception {
        stubExists(true);

        client.createIndex();

        verify(indicesClient, never()).create(any(Function.class));
    }

    @Test
    @DisplayName("batchSave：索引不存在时自动创建并批量写入")
    void batchSave_autoCreateAndWrite() throws Exception {
        stubExists(false);
        when(indicesClient.create(any(Function.class))).thenReturn(null);
        when(elasticsearchClient.bulk(any(BulkRequest.class))).thenReturn(null);

        SessionMemoryDocument doc = SessionMemoryDocument.builder()
                .sessionId("100")
                .aggregationType(AggregationType.GROUP)
                .aggregationStartSeq(1)
                .aggregationEndSeq(3)
                .aggregationText("摘要")
                .vector(List.of(0.1f, 0.2f))
                .build();

        client.batchSave(List.of(doc));

        verify(indicesClient).create(any(Function.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<BulkRequest> bulkCaptor = ArgumentCaptor.forClass(BulkRequest.class);
        verify(elasticsearchClient).bulk(bulkCaptor.capture());
        BulkRequest bulkRequest = bulkCaptor.getValue();
        assertEquals(1, bulkRequest.operations().size());
        BulkOperation op = bulkRequest.operations().get(0);
        assertTrue(op.isIndex(), "应为 index 操作");
        IndexOperation<?> idx = op.index();
        assertEquals(SessionMemoryESClient.INDEX_NAME, idx.index());
        assertEquals("100_GROUP_1_3", idx.id());
    }

    @Test
    @DisplayName("batchSave：空列表不写入")
    void batchSave_empty_skip() throws Exception {
        stubExists(true);

        client.batchSave(List.of());

        verify(elasticsearchClient, never()).bulk(any(BulkRequest.class));
    }

    @Test
    @DisplayName("batchSave：IOException 包装为 IllegalStateException")
    void batchSave_ioException() throws Exception {
        stubExists(true);
        when(elasticsearchClient.bulk(any(BulkRequest.class))).thenThrow(new IOException("boom"));

        SessionMemoryDocument doc = SessionMemoryDocument.builder()
                .sessionId("100")
                .aggregationType(AggregationType.GROUP)
                .aggregationStartSeq(1)
                .aggregationEndSeq(3)
                .aggregationText("摘要")
                .vector(List.of(0.1f))
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> client.batchSave(List.of(doc)));
        assertTrue(ex.getMessage().contains(SessionMemoryESClient.INDEX_NAME));
    }

    @Test
    @DisplayName("checkDailyExists：查询 term sessionId + term aggregationType=DAILY + 时间范围重叠，无命中返回 false")
    void checkDailyExists_noHit_returnsFalse() throws Exception {
        stubExists(true);
        @SuppressWarnings("unchecked")
        SearchResponse<SessionMemoryDocument> searchResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<SessionMemoryDocument> hitsMeta = mock(HitsMetadata.class);
        when(hitsMeta.hits()).thenReturn(List.of());
        when(searchResponse.hits()).thenReturn(hitsMeta);
        when(elasticsearchClient.search(any(Function.class), eq(SessionMemoryDocument.class)))
                .thenReturn(searchResponse);

        boolean exists = client.checkDailyExists("100", 1000L, 2000L);
        assertFalse(exists);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>> captor =
                ArgumentCaptor.forClass(Function.class);
        verify(elasticsearchClient).search(captor.capture(), eq(SessionMemoryDocument.class));
        SearchRequest request = apply(captor.getValue(), new SearchRequest.Builder());
        assertEquals(List.of(SessionMemoryESClient.INDEX_NAME), request.index());
        assertEquals(1, request.size());
        Query query = request.query();
        assertTrue(query.isBool(), "应为 bool 查询");
        assertNotNull(query.bool().must());
        assertEquals(4, query.bool().must().size());

        Query startRange = query.bool().must().get(2);
        assertTrue(startRange.isRange(), "第3个条件应为 aggregationStartTime range");
        assertNotNull(startRange.range().number().lt(), "aggregationStartTime 应使用 lt（半开区间终点）");
        assertNull(startRange.range().number().lte(), "aggregationStartTime 不应使用 lte");

        Query endRange = query.bool().must().get(3);
        assertTrue(endRange.isRange(), "第4个条件应为 aggregationEndTime range");
        assertNotNull(endRange.range().number().gt(), "aggregationEndTime 应使用 gt（半开区间起点）");
        assertNull(endRange.range().number().gte(), "aggregationEndTime 不应使用 gte");
    }

    @Test
    @DisplayName("checkDailyExists：有重叠 DAILY 文档时返回 true")
    void checkDailyExists_hit_returnsTrue() throws Exception {
        stubExists(true);
        @SuppressWarnings("unchecked")
        SearchResponse<SessionMemoryDocument> searchResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<SessionMemoryDocument> hitsMeta = mock(HitsMetadata.class);
        @SuppressWarnings("unchecked")
        Hit<SessionMemoryDocument> hit = mock(Hit.class);
        when(hitsMeta.hits()).thenReturn(List.of(hit));
        when(searchResponse.hits()).thenReturn(hitsMeta);
        when(elasticsearchClient.search(any(Function.class), eq(SessionMemoryDocument.class)))
                .thenReturn(searchResponse);

        boolean exists = client.checkDailyExists("100", 1000L, 2000L);
        assertTrue(exists);
    }

    @Test
    @DisplayName("checkDailyExists：索引不存在时先自动创建再查询")
    void checkDailyExists_autoCreateIndex() throws Exception {
        stubExists(false);
        when(indicesClient.create(any(Function.class))).thenReturn(null);
        @SuppressWarnings("unchecked")
        SearchResponse<SessionMemoryDocument> searchResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<SessionMemoryDocument> hitsMeta = mock(HitsMetadata.class);
        when(hitsMeta.hits()).thenReturn(List.of());
        when(searchResponse.hits()).thenReturn(hitsMeta);
        when(elasticsearchClient.search(any(Function.class), eq(SessionMemoryDocument.class)))
                .thenReturn(searchResponse);

        client.checkDailyExists("100", 1000L, 2000L);

        verify(indicesClient).create(any(Function.class));
        verify(elasticsearchClient).search(any(Function.class), eq(SessionMemoryDocument.class));
    }

    @Test
    @DisplayName("checkDailyExists：IOException 包装为 IllegalStateException")
    void checkDailyExists_ioException() throws Exception {
        stubExists(true);
        when(elasticsearchClient.search(any(Function.class), eq(SessionMemoryDocument.class)))
                .thenThrow(new IOException("boom"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> client.checkDailyExists("100", 1000L, 2000L));
        assertTrue(ex.getMessage().contains(SessionMemoryESClient.INDEX_NAME));
    }

    @Test
    @DisplayName("queryBySessionId：bool 查询含 term(sessionId)+term(aggregationType)，sort 降序，分页 from/size 正确，返回文档与总数")
    void queryBySessionId_returnsDocuments() throws Exception {
        stubExists(true);
        @SuppressWarnings("unchecked")
        SearchResponse<SessionMemoryDocument> searchResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<SessionMemoryDocument> hitsMeta = mock(HitsMetadata.class);
        @SuppressWarnings("unchecked")
        Hit<SessionMemoryDocument> hit = mock(Hit.class);
        SessionMemoryDocument doc = SessionMemoryDocument.builder()
                .sessionId("100")
                .aggregationType(AggregationType.GROUP)
                .aggregationStartSeq(1)
                .aggregationEndSeq(3)
                .aggregationText("摘要")
                .build();
        when(hit.source()).thenReturn(doc);
        when(hitsMeta.hits()).thenReturn(List.of(hit));
        TotalHits totalHits = TotalHits.of(t -> t.value(5L).relation(TotalHitsRelation.Eq));
        when(hitsMeta.total()).thenReturn(totalHits);
        when(searchResponse.hits()).thenReturn(hitsMeta);
        when(elasticsearchClient.search(any(Function.class), eq(SessionMemoryDocument.class)))
                .thenReturn(searchResponse);

        PageResult<SessionMemoryDocument> result = client.queryBySessionId("100", AggregationType.GROUP, 2, 10);

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertSame(doc, result.getList().get(0));
        assertEquals(5L, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(10, result.getSize());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>> captor =
                ArgumentCaptor.forClass(Function.class);
        verify(elasticsearchClient).search(captor.capture(), eq(SessionMemoryDocument.class));
        SearchRequest request = apply(captor.getValue(), new SearchRequest.Builder());
        assertEquals(List.of(SessionMemoryESClient.INDEX_NAME), request.index());

        Query query = request.query();
        assertTrue(query.isBool(), "应为 bool 查询");
        assertNotNull(query.bool().must());
        assertEquals(2, query.bool().must().size(), "bool 查询应包含 2 个 must 条件");

        Query termSession = query.bool().must().get(0);
        assertTrue(termSession.isTerm(), "第1个条件应为 term 查询");
        assertEquals("sessionId", termSession.term().field());
        assertEquals("100", termSession.term().value().stringValue());

        Query termType = query.bool().must().get(1);
        assertTrue(termType.isTerm(), "第2个条件应为 term 查询");
        assertEquals("aggregationType", termType.term().field());
        assertEquals("GROUP", termType.term().value().stringValue());

        assertNotNull(request.sort());
        assertEquals(1, request.sort().size(), "应只包含 1 个排序字段");
        SortOptions sort = request.sort().get(0);
        assertTrue(sort.isField(), "应为 field 排序");
        FieldSort fieldSort = sort.field();
        assertEquals("aggregationStartTime", fieldSort.field());
        assertEquals(SortOrder.Desc, fieldSort.order());

        assertEquals(10, request.from(), "from=(page-1)*size=(2-1)*10=10");
        assertEquals(10, request.size());
    }

    @Test
    @DisplayName("queryBySessionId：source() 为 null 的命中被过滤，只返回有效文档")
    void queryBySessionId_filtersNullSource() throws Exception {
        stubExists(true);
        @SuppressWarnings("unchecked")
        SearchResponse<SessionMemoryDocument> searchResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<SessionMemoryDocument> hitsMeta = mock(HitsMetadata.class);
        @SuppressWarnings("unchecked")
        Hit<SessionMemoryDocument> validHit = mock(Hit.class);
        @SuppressWarnings("unchecked")
        Hit<SessionMemoryDocument> nullHit = mock(Hit.class);
        SessionMemoryDocument doc = SessionMemoryDocument.builder().sessionId("100").build();
        when(validHit.source()).thenReturn(doc);
        when(nullHit.source()).thenReturn(null);
        when(hitsMeta.hits()).thenReturn(List.of(validHit, nullHit));
        when(hitsMeta.total()).thenReturn(TotalHits.of(t -> t.value(2L).relation(TotalHitsRelation.Eq)));
        when(searchResponse.hits()).thenReturn(hitsMeta);
        when(elasticsearchClient.search(any(Function.class), eq(SessionMemoryDocument.class)))
                .thenReturn(searchResponse);

        PageResult<SessionMemoryDocument> result = client.queryBySessionId("100", AggregationType.DAILY, 1, 20);

        assertEquals(1, result.getList().size(), "source()==null 的命中应被过滤");
        assertSame(doc, result.getList().get(0));
        assertEquals(2L, result.getTotal());
    }

    @Test
    @DisplayName("queryBySessionId：total 为 null 时返回 0")
    void queryBySessionId_nullTotal_returnsZero() throws Exception {
        stubExists(true);
        @SuppressWarnings("unchecked")
        SearchResponse<SessionMemoryDocument> searchResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<SessionMemoryDocument> hitsMeta = mock(HitsMetadata.class);
        when(hitsMeta.hits()).thenReturn(List.of());
        when(hitsMeta.total()).thenReturn(null);
        when(searchResponse.hits()).thenReturn(hitsMeta);
        when(elasticsearchClient.search(any(Function.class), eq(SessionMemoryDocument.class)))
                .thenReturn(searchResponse);

        PageResult<SessionMemoryDocument> result = client.queryBySessionId("100", AggregationType.GROUP, 1, 20);

        assertNotNull(result.getList());
        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal(), "total 为 null 时应返回 0");
    }

    @Test
    @DisplayName("queryBySessionId：page/size 为 0 或负数时使用 Math.max 兜底（page>=1、size>=1）")
    void queryBySessionId_safePageAndSize() throws Exception {
        stubExists(true);
        @SuppressWarnings("unchecked")
        SearchResponse<SessionMemoryDocument> searchResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<SessionMemoryDocument> hitsMeta = mock(HitsMetadata.class);
        when(hitsMeta.hits()).thenReturn(List.of());
        when(hitsMeta.total()).thenReturn(TotalHits.of(t -> t.value(0L).relation(TotalHitsRelation.Eq)));
        when(searchResponse.hits()).thenReturn(hitsMeta);
        when(elasticsearchClient.search(any(Function.class), eq(SessionMemoryDocument.class)))
                .thenReturn(searchResponse);

        PageResult<SessionMemoryDocument> result = client.queryBySessionId("100", AggregationType.DAILY, 0, 0);

        assertEquals(1, result.getPage(), "page 至少为 1");
        assertEquals(1, result.getSize(), "size 至少为 1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>> captor =
                ArgumentCaptor.forClass(Function.class);
        verify(elasticsearchClient).search(captor.capture(), eq(SessionMemoryDocument.class));
        SearchRequest request = apply(captor.getValue(), new SearchRequest.Builder());
        assertEquals(0, request.from(), "from=(max(1,0)-1)*max(1,0)=0");
        assertEquals(1, request.size());
    }

    @Test
    @DisplayName("queryBySessionId：负数 page/size 同样兜底为 1")
    void queryBySessionId_negativePageSize() throws Exception {
        stubExists(true);
        @SuppressWarnings("unchecked")
        SearchResponse<SessionMemoryDocument> searchResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<SessionMemoryDocument> hitsMeta = mock(HitsMetadata.class);
        when(hitsMeta.hits()).thenReturn(List.of());
        when(hitsMeta.total()).thenReturn(TotalHits.of(t -> t.value(0L).relation(TotalHitsRelation.Eq)));
        when(searchResponse.hits()).thenReturn(hitsMeta);
        when(elasticsearchClient.search(any(Function.class), eq(SessionMemoryDocument.class)))
                .thenReturn(searchResponse);

        PageResult<SessionMemoryDocument> result = client.queryBySessionId("100", AggregationType.GROUP, -5, -2);

        assertEquals(1, result.getPage());
        assertEquals(1, result.getSize());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>> captor =
                ArgumentCaptor.forClass(Function.class);
        verify(elasticsearchClient).search(captor.capture(), eq(SessionMemoryDocument.class));
        SearchRequest request = apply(captor.getValue(), new SearchRequest.Builder());
        assertEquals(0, request.from());
        assertEquals(1, request.size());
    }

    @Test
    @DisplayName("queryBySessionId：索引不存在时先自动创建再查询")
    void queryBySessionId_autoCreateIndex() throws Exception {
        stubExists(false);
        when(indicesClient.create(any(Function.class))).thenReturn(null);
        @SuppressWarnings("unchecked")
        SearchResponse<SessionMemoryDocument> searchResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<SessionMemoryDocument> hitsMeta = mock(HitsMetadata.class);
        when(hitsMeta.hits()).thenReturn(List.of());
        when(hitsMeta.total()).thenReturn(TotalHits.of(t -> t.value(0L).relation(TotalHitsRelation.Eq)));
        when(searchResponse.hits()).thenReturn(hitsMeta);
        when(elasticsearchClient.search(any(Function.class), eq(SessionMemoryDocument.class)))
                .thenReturn(searchResponse);

        client.queryBySessionId("100", AggregationType.GROUP, 1, 20);

        verify(indicesClient).create(any(Function.class));
        verify(elasticsearchClient).search(any(Function.class), eq(SessionMemoryDocument.class));
    }

    @Test
    @DisplayName("updateDocument：索引不存在时先自动创建，按 docId 更新 aggregationText 与 vector")
    void updateDocument_buildsUpdateRequest() throws Exception {
        stubExists(false);
        when(indicesClient.create(any(Function.class))).thenReturn(null);
        @SuppressWarnings("unchecked")
        UpdateResponse<SessionMemoryDocument> updateResponse = mock(UpdateResponse.class);
        when(elasticsearchClient.<SessionMemoryDocument, JsonData>update(any(Function.class),
                eq(SessionMemoryDocument.class))).thenReturn(updateResponse);

        client.updateDocument("100_GROUP_1_3", "新摘要", List.of(0.3f, 0.4f));

        verify(indicesClient).create(any(Function.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<UpdateRequest.Builder<SessionMemoryDocument, JsonData>,
                ObjectBuilder<UpdateRequest<SessionMemoryDocument, JsonData>>>> captor =
                ArgumentCaptor.forClass(Function.class);
        verify(elasticsearchClient).<SessionMemoryDocument, JsonData>update(captor.capture(),
                eq(SessionMemoryDocument.class));
        UpdateRequest<SessionMemoryDocument, JsonData> request =
                apply(captor.getValue(), new UpdateRequest.Builder<SessionMemoryDocument, JsonData>());
        assertEquals(SessionMemoryESClient.INDEX_NAME, request.index());
        assertEquals("100_GROUP_1_3", request.id());
        assertNotNull(request.doc(), "update 应包含部分文档");
    }

    @Test
    @DisplayName("updateDocument：部分文档仅包含 aggregationText 与 vector 字段")
    void updateDocument_docContainsOnlyTextAndVector() throws Exception {
        stubExists(true);
        @SuppressWarnings("unchecked")
        UpdateResponse<SessionMemoryDocument> updateResponse = mock(UpdateResponse.class);
        when(elasticsearchClient.<SessionMemoryDocument, JsonData>update(any(Function.class),
                eq(SessionMemoryDocument.class))).thenReturn(updateResponse);

        client.updateDocument("100_GROUP_1_3", "新摘要", List.of(0.3f, 0.4f));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<UpdateRequest.Builder<SessionMemoryDocument, JsonData>,
                ObjectBuilder<UpdateRequest<SessionMemoryDocument, JsonData>>>> captor =
                ArgumentCaptor.forClass(Function.class);
        verify(elasticsearchClient).<SessionMemoryDocument, JsonData>update(captor.capture(),
                eq(SessionMemoryDocument.class));
        UpdateRequest<SessionMemoryDocument, JsonData> request =
                apply(captor.getValue(), new UpdateRequest.Builder<SessionMemoryDocument, JsonData>());
        JsonData doc = request.doc();
        assertNotNull(doc);
        JsonpMapper mapper = new JacksonJsonpMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> partial = doc.to(Map.class, mapper);
        assertEquals("新摘要", partial.get("aggregationText"));
        @SuppressWarnings("unchecked")
        List<Number> vector = (List<Number>) partial.get("vector");
        assertNotNull(vector);
        assertEquals(2, vector.size());
        assertEquals(0.3, vector.get(0).doubleValue(), 1e-6);
        assertEquals(0.4, vector.get(1).doubleValue(), 1e-6);
    }

    @Test
    @DisplayName("updateDocument：索引已存在时不重复创建")
    void updateDocument_indexExists_skipCreate() throws Exception {
        stubExists(true);
        @SuppressWarnings("unchecked")
        UpdateResponse<SessionMemoryDocument> updateResponse = mock(UpdateResponse.class);
        when(elasticsearchClient.<SessionMemoryDocument, JsonData>update(any(Function.class),
                eq(SessionMemoryDocument.class))).thenReturn(updateResponse);

        client.updateDocument("100_GROUP_1_3", "新摘要", List.of(0.3f, 0.4f));

        verify(indicesClient, never()).create(any(Function.class));
        verify(elasticsearchClient).<SessionMemoryDocument, JsonData>update(any(Function.class),
                eq(SessionMemoryDocument.class));
    }

    @Test
    @DisplayName("updateDocument：IOException 包装为 IllegalStateException 且消息含索引名")
    void updateDocument_ioException() throws Exception {
        stubExists(true);
        when(elasticsearchClient.<SessionMemoryDocument, JsonData>update(any(Function.class),
                eq(SessionMemoryDocument.class))).thenThrow(new IOException("boom"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> client.updateDocument("100_GROUP_1_3", "新摘要", List.of(0.3f, 0.4f)));
        assertTrue(ex.getMessage().contains(SessionMemoryESClient.INDEX_NAME));
    }
}
