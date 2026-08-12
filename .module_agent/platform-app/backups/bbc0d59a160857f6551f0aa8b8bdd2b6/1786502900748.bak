package com.ghost616.platform.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorProperty;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.util.ObjectBuilder;
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
    @DisplayName("createIndex：索引不存在时创建，mapping 含 5 字段")
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
        assertEquals(5, properties.size());

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
        assertTrue(properties.get("aggregationStartSeq").isInteger(), "aggregationStartSeq 应为 integer");
        assertTrue(properties.get("aggregationEndSeq").isInteger(), "aggregationEndSeq 应为 integer");
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
        assertEquals("100_1_3", idx.id());
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
                .aggregationStartSeq(1)
                .aggregationEndSeq(3)
                .aggregationText("摘要")
                .vector(List.of(0.1f))
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> client.batchSave(List.of(doc)));
        assertTrue(ex.getMessage().contains(SessionMemoryESClient.INDEX_NAME));
    }
}
