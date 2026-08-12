package com.ghost616.platform.service.knowledge;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghost616.agentbase.dto.model.EmbeddingRequest;
import com.ghost616.agentbase.dto.model.EmbeddingResponse;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import com.ghost616.agentinteg.knowledge.FileInfo;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseInfo;
import com.ghost616.agentinteg.knowledge.SearchType;
import com.ghost616.agentinteg.knowledge.TextChunkWithFile;
import com.ghost616.platform.entity.AgentKnowledgeBase;
import com.ghost616.platform.entity.KnowledgeBase;
import com.ghost616.platform.entity.KnowledgeFile;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.enums.PublishStatus;
import com.ghost616.platform.model.TextChunk;
import com.ghost616.platform.repository.AgentKnowledgeBaseMapper;
import com.ghost616.platform.repository.KnowledgeBaseMapper;
import com.ghost616.platform.repository.KnowledgeFileMapper;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.service.search.KnowledgeSearchClient;
import com.ghost616.platform.util.IdConverter;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseQueryProviderImplTest {

    @Mock
    private SessionMapper sessionMapper;
    @Mock
    private AgentKnowledgeBaseMapper agentKnowledgeBaseMapper;
    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;
    @Mock
    private KnowledgeFileMapper knowledgeFileMapper;
    @Mock
    private ModelConfigMapper modelConfigMapper;
    @Mock
    private ModelInvokerManager modelInvokerManager;
    @Mock
    private KnowledgeSearchClient knowledgeSearchClient;
    @Mock
    private ModelInvoker modelInvoker;

    private KnowledgeBaseQueryProviderImpl provider;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Session.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AgentKnowledgeBase.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), KnowledgeBase.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), KnowledgeFile.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ModelConfig.class);
        provider = new KnowledgeBaseQueryProviderImpl(sessionMapper, agentKnowledgeBaseMapper,
                knowledgeBaseMapper, knowledgeFileMapper, modelConfigMapper,
                modelInvokerManager, knowledgeSearchClient);
    }

    private Session session(Long id, Long agentId) {
        Session s = new Session();
        s.setId(id);
        s.setAgentId(agentId);
        return s;
    }

    private AgentKnowledgeBase binding(Long kbId) {
        AgentKnowledgeBase b = new AgentKnowledgeBase();
        b.setId(1L);
        b.setAgentId(10L);
        b.setKnowledgeBaseId(kbId);
        return b;
    }

    private KnowledgeBase kb(Long id, String esIndex) {
        KnowledgeBase k = new KnowledgeBase();
        k.setId(id);
        k.setName("kb-name");
        k.setDescription("kb-desc");
        k.setEsIndex(esIndex);
        k.setVectorModelId(5L);
        return k;
    }

    private KnowledgeFile file(Long id, Long kbId, String name, String content) {
        KnowledgeFile f = new KnowledgeFile();
        f.setId(id);
        f.setKnowledgeBaseId(kbId);
        f.setFileName(name);
        f.setFileDescription("desc-" + name);
        f.setFileContent(content);
        f.setPublishStatus(PublishStatus.PUBLISHED);
        return f;
    }

    private ModelConfig modelConfig(Long id) {
        ModelConfig m = new ModelConfig();
        m.setId(id);
        m.setModelName("embed-model");
        m.setApiKey("key");
        m.setBaseUrl("http://localhost");
        m.setTemperature(0.1);
        m.setMaxTokens(1024);
        return m;
    }

    private TextChunk chunk(Long kbId, Long fileId, int line, String text) {
        return TextChunk.builder()
                .knowledgeBaseId(kbId)
                .fileId(fileId)
                .lineNumber(line)
                .text(text)
                .build();
    }

    // ---------- getKnowledgeBaseInfo ----------

    @Test
    @DisplayName("getKnowledgeBaseInfo: sessionId 为 null/空白时返回空列表")
    void getKnowledgeBaseInfo_nullSessionId() {
        assertTrue(provider.getKnowledgeBaseInfo(null).isEmpty());
        assertTrue(provider.getKnowledgeBaseInfo("  ").isEmpty());
        verifyNoInteractions(sessionMapper);
    }

    @Test
    @DisplayName("getKnowledgeBaseInfo: sessionId 非法时 IdConverter 抛 IllegalArgumentException")
    void getKnowledgeBaseInfo_invalidSessionId() {
        assertThrows(IllegalArgumentException.class, () -> provider.getKnowledgeBaseInfo("abc"));
    }

    @Test
    @DisplayName("getKnowledgeBaseInfo: session 不存在时返回空列表")
    void getKnowledgeBaseInfo_sessionNotFound() {
        when(sessionMapper.selectById(100L)).thenReturn(null);
        assertTrue(provider.getKnowledgeBaseInfo("100").isEmpty());
        verify(agentKnowledgeBaseMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("getKnowledgeBaseInfo: session.agentId 为 null 时返回空列表")
    void getKnowledgeBaseInfo_agentIdNull() {
        when(sessionMapper.selectById(100L)).thenReturn(session(100L, null));
        assertTrue(provider.getKnowledgeBaseInfo("100").isEmpty());
        verify(agentKnowledgeBaseMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("getKnowledgeBaseInfo: 无绑定记录时返回空列表")
    void getKnowledgeBaseInfo_noBinding() {
        when(sessionMapper.selectById(100L)).thenReturn(session(100L, 10L));
        when(agentKnowledgeBaseMapper.selectList(any())).thenReturn(List.of());
        assertTrue(provider.getKnowledgeBaseInfo("100").isEmpty());
        verify(knowledgeBaseMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("getKnowledgeBaseInfo: 绑定知识库不存在时返回空列表")
    void getKnowledgeBaseInfo_bindingKbNotFound() {
        when(sessionMapper.selectById(100L)).thenReturn(session(100L, 10L));
        when(agentKnowledgeBaseMapper.selectList(any())).thenReturn(List.of(binding(200L)));
        when(knowledgeBaseMapper.selectById(200L)).thenReturn(null);
        assertTrue(provider.getKnowledgeBaseInfo("100").isEmpty());
    }

    @Test
    @DisplayName("getKnowledgeBaseInfo: 正常链路返回知识库信息列表")
    void getKnowledgeBaseInfo_normal() {
        when(sessionMapper.selectById(100L)).thenReturn(session(100L, 10L));
        when(agentKnowledgeBaseMapper.selectList(any())).thenReturn(List.of(binding(200L)));
        when(knowledgeBaseMapper.selectById(200L)).thenReturn(kb(200L, "idx"));
        List<KnowledgeBaseInfo> infos = provider.getKnowledgeBaseInfo("100");
        assertEquals(1, infos.size());
        KnowledgeBaseInfo info = infos.get(0);
        assertNotNull(info);
        assertEquals("200", info.kbId());
        assertEquals("kb-name", info.kbName());
        assertEquals("kb-desc", info.kbDescription());
    }

    @Test
    @DisplayName("getKnowledgeBaseInfo: 多绑定记录时返回全部存在的知识库信息")
    void getKnowledgeBaseInfo_multipleBindings() {
        when(sessionMapper.selectById(100L)).thenReturn(session(100L, 10L));
        AgentKnowledgeBase first = binding(200L);
        first.setId(1L);
        AgentKnowledgeBase second = binding(300L);
        second.setId(2L);
        when(agentKnowledgeBaseMapper.selectList(any())).thenReturn(List.of(first, second));
        when(knowledgeBaseMapper.selectById(200L)).thenReturn(kb(200L, "idx"));
        when(knowledgeBaseMapper.selectById(300L)).thenReturn(kb(300L, "idx2"));

        List<KnowledgeBaseInfo> infos = provider.getKnowledgeBaseInfo("100");

        assertEquals(2, infos.size());
        assertEquals("200", infos.get(0).kbId());
        assertEquals("300", infos.get(1).kbId());
        verify(knowledgeBaseMapper).selectById(200L);
        verify(knowledgeBaseMapper).selectById(300L);
    }

    // ---------- searchFiles ----------

    @Test
    @DisplayName("searchFiles: 仅查询已发布到 ES 的文件（publish_status 过滤）")
    void searchFiles_filtersPublished() {
        when(knowledgeFileMapper.selectList(any())).thenReturn(List.of(file(1L, 100L, "a.txt", "a\nb")));
        provider.searchFiles("100", null, 10);

        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeFileMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("publish_status"), sql);
    }

    @Test
    @DisplayName("searchFiles: fileName 非空时追加 like 条件")
    void searchFiles_withFileName() {
        when(knowledgeFileMapper.selectList(any())).thenReturn(
                List.of(file(1L, 100L, "readme.md", "a\nb\nc")));
        List<FileInfo> result = provider.searchFiles("100", "readme", 10);

        assertEquals(1, result.size());
        FileInfo info = result.get(0);
        assertEquals("1", info.fileId());
        assertEquals("readme.md", info.fileName());
        assertEquals(3, info.maxLineCount());

        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeFileMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("file_name") && sql.contains("LIKE"), sql);
    }

    @Test
    @DisplayName("searchFiles: fileName 为 null/空时不加 like 条件")
    void searchFiles_nullFileName() {
        when(knowledgeFileMapper.selectList(any())).thenReturn(List.of(file(1L, 100L, "a.txt", "")));
        provider.searchFiles("100", null, 10);
        provider.searchFiles("100", "  ", 10);

        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeFileMapper, times(2)).selectList(captor.capture());
        for (LambdaQueryWrapper wrapper : captor.getAllValues()) {
            String sql = wrapper.getSqlSegment();
            assertFalse(sql.contains("LIKE"), sql);
        }
    }

    @Test
    @DisplayName("searchFiles: 按 createTime 降序且 SQL 层追加 LIMIT")
    void searchFiles_limitAndOrder() {
        when(knowledgeFileMapper.selectList(any())).thenReturn(List.of(
                file(1L, 100L, "c.txt", null),
                file(2L, 100L, "b.txt", null),
                file(3L, 100L, "a.txt", null)));
        List<FileInfo> result = provider.searchFiles("100", null, 2);
        assertEquals(3, result.size());
        assertEquals("1", result.get(0).fileId());

        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeFileMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("create_time") && sql.contains("DESC"), sql);
        assertTrue(sql.toUpperCase().contains("LIMIT 2"), sql);
    }

    @Test
    @DisplayName("searchFiles: limit<=0 时直接返回空列表且不触发查询")
    void searchFiles_nonPositiveLimit() {
        List<FileInfo> result0 = provider.searchFiles("100", null, 0);
        List<FileInfo> resultNeg = provider.searchFiles("100", null, -5);

        assertEquals(List.of(), result0);
        assertEquals(List.of(), resultNeg);
        verify(knowledgeFileMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("searchFiles: 空内容行数计算为 0")
    void searchFiles_emptyContentLineCount() {
        when(knowledgeFileMapper.selectList(any())).thenReturn(
                List.of(file(1L, 100L, "e.txt", null)));
        List<FileInfo> result = provider.searchFiles("100", null, 10);
        assertEquals(0, result.get(0).maxLineCount());
    }

    // ---------- searchChunks ----------

    @Test
    @DisplayName("searchChunks: 知识库不存在时返回空列表")
    void searchChunks_kbNotFound() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(null);
        assertEquals(List.of(), provider.searchChunks("100", null, SearchType.VECTOR, "q", 10));
    }

    @Test
    @DisplayName("searchChunks: esIndex 为空时返回空列表")
    void searchChunks_emptyEsIndex() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "  "));
        assertEquals(List.of(), provider.searchChunks("100", null, SearchType.VECTOR, "q", 10));
    }

    @Test
    @DisplayName("searchChunks: vectorModelId 缺失时抛 MODEL_NOT_FOUND")
    void searchChunks_vectorModelIdMissing() {
        KnowledgeBase k = kb(100L, "idx");
        k.setVectorModelId(null);
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(k);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> provider.searchChunks("100", null, SearchType.VECTOR, "q", 10));
        assertEquals(ErrorCode.MODEL_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("searchChunks: vector 检索正常链路，embed 调用与向量检索")
    void searchChunks_vectorSearch() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(modelConfigMapper.selectById(5L)).thenReturn(modelConfig(5L));
        when(modelInvokerManager.getInvoker(any())).thenReturn(modelInvoker);
        EmbeddingResponse resp = EmbeddingResponse.builder()
                .embeddings(List.of(EmbeddingResponse.EmbeddingItem.builder()
                        .index(0).embedding(List.of(0.1f, 0.2f)).build()))
                .build();
        when(modelInvoker.embed(any(EmbeddingRequest.class))).thenReturn(resp);
        when(knowledgeSearchClient.vectorSearch(eq("idx"), eq(100L), isNull(), eq(List.of(0.1f, 0.2f)), eq(5)))
                .thenReturn(List.of(chunk(100L, 2L, 5, "line5")));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(file(2L, 100L, "a.txt", "x"));

        List<TextChunkWithFile> result = provider.searchChunks("100", null, SearchType.VECTOR, "q", 5);

        assertEquals(1, result.size());
        TextChunkWithFile withFile = result.get(0);
        assertEquals("2", withFile.fileId());
        assertEquals("a.txt", withFile.fileName());
        assertEquals(1, withFile.chunkList().size());
        assertEquals(5, withFile.chunkList().get(0).lineNumber());
        assertEquals("line5", withFile.chunkList().get(0).text());
        ArgumentCaptor<EmbeddingRequest> reqCaptor = ArgumentCaptor.forClass(EmbeddingRequest.class);
        verify(modelInvoker).embed(reqCaptor.capture());
        assertEquals("embed-model", reqCaptor.getValue().getModel());
        assertEquals(List.of("q"), reqCaptor.getValue().getInputList());
    }

    @Test
    @DisplayName("searchChunks: full_text 检索调用全文检索")
    void searchChunks_fullTextSearch() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(knowledgeSearchClient.fullTextSearch("idx", 100L, null, "q", 5))
                .thenReturn(List.of(chunk(100L, 2L, 3, "line3")));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(file(2L, 100L, "a.txt", "x"));

        List<TextChunkWithFile> result = provider.searchChunks("100", null, SearchType.FULLTEXT, "q", 5);
        assertEquals(1, result.size());
        assertEquals("2", result.get(0).fileId());
        verify(knowledgeSearchClient, never()).vectorSearch(any(), any(), any(), anyList(), anyInt());
    }

    @Test
    @DisplayName("searchChunks: fileId 透传 ES 查询，非内存过滤")
    void searchChunks_fileIdPassedToEs() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(knowledgeSearchClient.fullTextSearch("idx", 100L, 99L, "q", 5))
                .thenReturn(new java.util.ArrayList<>(List.of(chunk(100L, 2L, 3, "line3"))));

        List<TextChunkWithFile> result = provider.searchChunks("100", "99", SearchType.FULLTEXT, "q", 5);

        assertEquals(1, result.size());
        verify(knowledgeSearchClient).fullTextSearch("idx", 100L, 99L, "q", 5);
        verify(knowledgeFileMapper).selectById(2L);
    }

    @Test
    @DisplayName("searchChunks: fileId 为空时 ES 查询不携带 fileId 过滤")
    void searchChunks_fileIdNullNoFilter() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(knowledgeSearchClient.fullTextSearch("idx", 100L, null, "q", 5))
                .thenReturn(new java.util.ArrayList<>(List.of(chunk(100L, 2L, 3, "line3"))));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(file(2L, 100L, "a.txt", "x"));

        List<TextChunkWithFile> result = provider.searchChunks("100", null, SearchType.FULLTEXT, "q", 5);

        assertEquals(1, result.size());
        verify(knowledgeSearchClient).fullTextSearch("idx", 100L, null, "q", 5);
        verify(knowledgeFileMapper).selectById(2L);
    }

    @Test
    @DisplayName("searchChunks: 文件不存在时 fileName 使用 fileId 字符串")
    void searchChunks_fileNameFallback() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(knowledgeSearchClient.fullTextSearch("idx", 100L, null, "q", 5))
                .thenReturn(List.of(chunk(100L, 2L, 3, "line3")));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(null);

        List<TextChunkWithFile> result = provider.searchChunks("100", null, SearchType.FULLTEXT, "q", 5);
        assertEquals("2", result.get(0).fileName());
    }

    @Test
    @DisplayName("searchChunks: 同一文件内按 lineNumber 去重")
    void searchChunks_dedupeByLineNumber() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(knowledgeSearchClient.fullTextSearch("idx", 100L, null, "q", 5))
                .thenReturn(new java.util.ArrayList<>(List.of(
                        chunk(100L, 2L, 5, "line5"),
                        chunk(100L, 2L, 5, "line5-dupe"))));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(file(2L, 100L, "a.txt", "x"));

        List<TextChunkWithFile> result = provider.searchChunks("100", null, SearchType.FULLTEXT, "q", 5);
        List<TextChunkWithFile.TextChunk> chunks = result.get(0).chunkList();
        assertEquals(1, chunks.size());
        assertEquals(5, chunks.get(0).lineNumber());
        assertEquals("line5", chunks.get(0).text());
    }

    @Test
    @DisplayName("searchChunks: hybrid 按 kbId_fileId_lineNumber 去重合并")
    void searchChunks_hybridMerge() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(modelConfigMapper.selectById(5L)).thenReturn(modelConfig(5L));
        when(modelInvokerManager.getInvoker(any())).thenReturn(modelInvoker);
        when(modelInvoker.embed(any(EmbeddingRequest.class))).thenReturn(EmbeddingResponse.builder()
                .embeddings(List.of(EmbeddingResponse.EmbeddingItem.builder()
                        .index(0).embedding(List.of(0.1f)).build()))
                .build());
        // 向量与全文命中同一行（line 5）应去重，另全文命中 line 8
        when(knowledgeSearchClient.vectorSearch(eq("idx"), eq(100L), isNull(), anyList(), eq(5)))
                .thenReturn(List.of(chunk(100L, 2L, 5, "line5")));
        when(knowledgeSearchClient.fullTextSearch("idx", 100L, null, "q", 5))
                .thenReturn(List.of(chunk(100L, 2L, 5, "line5"), chunk(100L, 2L, 8, "line8")));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(file(2L, 100L, "a.txt", "x"));

        List<TextChunkWithFile> result = provider.searchChunks("100", null, SearchType.HYBRID, "q", 5);
        assertEquals(1, result.size());
        // 向量/全文去重后仅剩命中行 line5 与 line8，按行号去重返回
        assertEquals(2, result.get(0).chunkList().size());
    }

    // ---------- getFileChunks ----------

    @Test
    @DisplayName("getFileChunks: 知识库不存在或 esIndex 为空时返回 null")
    void getFileChunks_kbMissing() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(null);
        assertNull(provider.getFileChunks("100", "2", 0, 10));
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, ""));
        assertNull(provider.getFileChunks("100", "2", 0, 10));
        verify(knowledgeSearchClient, never()).searchByFileAndLineRange(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("getFileChunks: 正常返回文本块，fileName 取文件表")
    void getFileChunks_normal() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(file(2L, 100L, "a.txt", "x"));
        when(knowledgeSearchClient.searchByFileAndLineRange("idx", 100L, 2L, 0, 10))
                .thenReturn(List.of(chunk(100L, 2L, 0, "line0"), chunk(100L, 2L, 1, "line1")));

        TextChunkWithFile result = provider.getFileChunks("100", "2", 0, 10);
        assertNotNull(result);
        assertEquals("2", result.fileId());
        assertEquals("a.txt", result.fileName());
        assertEquals(2, result.chunkList().size());
        assertEquals(0, result.chunkList().get(0).lineNumber());
        assertEquals("line0", result.chunkList().get(0).text());
    }

    @Test
    @DisplayName("getFileChunks: 文件不存在时 fileName 使用 fileId 字符串")
    void getFileChunks_fileNameFallback() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(null);
        when(knowledgeSearchClient.searchByFileAndLineRange("idx", 100L, 2L, 0, 10))
                .thenReturn(List.of());

        TextChunkWithFile result = provider.getFileChunks("100", "2", 0, 10);
        assertNotNull(result);
        assertEquals("2", result.fileName());
        assertEquals(List.of(), result.chunkList());
    }
}
