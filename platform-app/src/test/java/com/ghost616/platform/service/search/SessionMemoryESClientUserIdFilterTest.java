package com.ghost616.platform.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 补充测试：SessionMemoryESClient 的 userId 过滤。
 *
 * <p>验证 queryBySessionId 在 userId 非空时向 bool 查询追加 term(userId) 过滤条件
 * （接口 GET /api/sessions/{id}/memory 的用户隔离落点），userId 为 null 时不追加。</p>
 */
@ExtendWith(MockitoExtension.class)
class SessionMemoryESClientUserIdFilterTest {

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

    private void stubEmptySearch() throws Exception {
        @SuppressWarnings("unchecked")
        SearchResponse<SessionMemoryDocument> searchResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<SessionMemoryDocument> hitsMeta = mock(HitsMetadata.class);
        when(hitsMeta.hits()).thenReturn(List.of());
        when(hitsMeta.total()).thenReturn(null);
        when(searchResponse.hits()).thenReturn(hitsMeta);
        when(elasticsearchClient.search(any(Function.class), eq(SessionMemoryDocument.class)))
                .thenReturn(searchResponse);
    }

    private Set<String> termFields(SearchRequest request) {
        Query query = request.query();
        assertTrue(query.isBool(), "应为 bool 查询");
        Set<String> fields = new HashSet<>();
        query.bool().filter().forEach(f -> {
            assertTrue(f.isTerm(), "过滤条件应为 term 查询");
            fields.add(f.term().field());
        });
        return fields;
    }

    @Test
    @DisplayName("queryBySessionId：userId 非空时追加 term(userId) 过滤")
    void queryBySessionId_非空userId_追加term过滤() throws Exception {
        stubExists(true);
        stubEmptySearch();

        client.queryBySessionId("100", 42L, AggregationType.GROUP, 1, 20);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>> captor =
                ArgumentCaptor.forClass(Function.class);
        verify(elasticsearchClient).search(captor.capture(), eq(SessionMemoryDocument.class));
        SearchRequest request = apply(captor.getValue(), new SearchRequest.Builder());

        Set<String> fields = termFields(request);
        assertEquals(Set.of("sessionId", "userId", "aggregationType"), fields,
                "userId 非空时应有 sessionId/userId/aggregationType 三个 term 过滤");

        Query userIdTerm = request.query().bool().filter().stream()
                .filter(f -> f.isTerm() && "userId".equals(f.term().field()))
                .findFirst().orElseThrow();
        assertEquals(42L, userIdTerm.term().value().longValue(), "userId term 值应为当前登录用户 ID");
    }

    @Test
    @DisplayName("queryBySessionId：userId 为 null 时不追加 userId 过滤")
    void queryBySessionId_nullUserId_不追加userId过滤() throws Exception {
        stubExists(true);
        stubEmptySearch();

        client.queryBySessionId("100", null, AggregationType.DAILY, 1, 20);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>> captor =
                ArgumentCaptor.forClass(Function.class);
        verify(elasticsearchClient).search(captor.capture(), eq(SessionMemoryDocument.class));
        SearchRequest request = apply(captor.getValue(), new SearchRequest.Builder());

        Set<String> fields = termFields(request);
        assertEquals(Set.of("sessionId", "aggregationType"), fields,
                "userId 为 null 时只应有 sessionId/aggregationType 两个 term 过滤");
    }
}