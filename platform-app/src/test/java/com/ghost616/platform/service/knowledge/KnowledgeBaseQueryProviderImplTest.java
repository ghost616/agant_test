package com.ghost616.platform.service.knowledge;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghost616.agentbase.dto.model.EmbeddingRequest;
import com.ghost616.agentbase.dto.model.EmbeddingResponse;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
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
    @DisplayName("getKnowledgeBaseInfo: sessionId 为 null/空白时返回 null")
    void getKnowledgeBaseInfo_nullSessionId() {
        assertNull(provider.getKnowledgeBaseInfo(null));
        assertNull(provider.getKnowledgeBaseInfo("  "));
        verifyNoInteractions(sessionMapper);
    }

    @Test
    @DisplayName("getKnowledgeBaseInfo: sessionId 非法时 IdConverter 抛 IllegalArgumentException")
    void getKnowledgeBaseInfo_invalidSessionId() {
        assertThrows(IllegalArgumentException.class, () -> provider.getKnowledgeBaseInfo("abc"));
    }

    @Test
    @DisplayName("getKnowledgeBaseInfo: session 不存在时返回 null")
    void getKnowledgeBaseInfo_sessionNotFound() {
        when(sessionMapper.selectById(100L)).thenReturn(null);
        assertNull(provider.getKnowledgeBaseInfo("100"));
        verify(agentKnowledgeBaseMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("getKnowledgeBaseInfo: session.agentId 为 null 时返回 null")
    void getKnowledgeBaseInfo_agentIdNull() {
        when(sessionMapper.selectById(100L)).thenReturn(session(100L, null));
        assertNull(provider.getKnowledgeBaseInfo("100"));
        verify(agentKnowledgeBaseMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("getKnowledgeBaseInfo: 无绑定记录时返回 null")
    void getKnowledgeBaseInfo_noBinding() {
        when(sessionMapper.selectById(100L)).thenReturn(session(100L, 10L));
        when(agentKnowledgeBaseMapper.selectList(any())).thenReturn(List.of());
        assertNull(provider.getKnowledgeBaseInfo("100"));
        verify(knowledgeBaseMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("getKnowledgeBaseInfo: 绑定知识库不存在时返回 null")
    void getKnowledgeBaseInfo_bindingKbNotFound() {
        when(sessionMapper.selectById(100L)).thenReturn(session(100L, 10L));
        when(agentKnowledgeBaseMapper.selectList(any())).thenReturn(List.of(binding(200L)));
        when(knowledgeBaseMapper.selectById(200L)).thenReturn(null);
        assertNull(provider.getKnowledgeBaseInfo("100"));
    }

    @Test
    @DisplayName("getKnowledgeBaseInfo: 正常链路返回知识库信息")
    void getKnowledgeBaseInfo_normal() {
        when(sessionMapper.selectById(100L)).thenReturn(session(100L, 10L));
        when(agentKnowledgeBaseMapper.selectList(any())).thenReturn(List.of(binding(200L)));
        when(knowledgeBaseMapper.selectById(200L)).thenReturn(kb(200L, "idx"));
        KnowledgeBaseInfo info = provider.getKnowledgeBaseInfo("100");
        assertNotNull(info);
        assertEquals(200L, info.kbId());
        assertEquals("kb-name", info.kbName());
        assertEquals("kb-desc", info.kbDescription());
    }

    @Test
    @DisplayName("getKnowledgeBaseInfo: 多绑定记录时仍取第一条")
    void getKnowledgeBaseInfo_multipleBindings() {
        when(sessionMapper.selectById(100L)).thenReturn(session(100L, 10L));
        AgentKnowledgeBase first = binding(200L);
        first.setId(1L);
        AgentKnowledgeBase second = binding(300L);
        second.setId(2L);
        when(agentKnowledgeBaseMapper.selectList(any())).thenReturn(List.of(first, second));
        when(knowledgeBaseMapper.selectById(200L)).thenReturn(kb(200L, "idx"));

        KnowledgeBaseInfo info = provider.getKnowledgeBaseInfo("100");

        assertNotNull(info);
        assertEquals(200L, info.kbId());
        verify(knowledgeBaseMapper).selectById(200L);
        verify(knowledgeBaseMapper, never()).selectById(300L);
    }

    // ---------- searchFiles ----------

    @Test
    @DisplayName("searchFiles: fileName 非空时追加 like 条件")
    void searchFiles_withFileName() {
        when(knowledgeFileMapper.selectList(any())).thenReturn(
                List.of(file(1L, 100L, "readme.md", "a\nb\nc")));
        List<FileInfo> result = provider.searchFiles(100L, "readme", 10);

        assertEquals(1, result.size());
        FileInfo info = result.get(0);
        assertEquals(1L, info.fileId());
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
        provider.searchFiles(100L, null, 10);
        provider.searchFiles(100L, "  ", 10);

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
        List<FileInfo> result = provider.searchFiles(100L, null, 2);
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).fileId());

        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeFileMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("create_time") && sql.contains("DESC"), sql);
        assertTrue(sql.toUpperCase().contains("LIMIT 2"), sql);
    }

    @Test
    @DisplayName("searchFiles: limit<=0 时直接返回空列表且不触发查询")
    void searchFiles_nonPositiveLimit() {
        List<FileInfo> result0 = provider.searchFiles(100L, null, 0);
        List<FileInfo> resultNeg = provider.searchFiles(100L, null, -5);

        assertEquals(List.of(), result0);
        assertEquals(List.of(), resultNeg);
        verify(knowledgeFileMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("searchFiles: 空内容行数计算为 0")
    void searchFiles_emptyContentLineCount() {
        when(knowledgeFileMapper.selectList(any())).thenReturn(
                List.of(file(1L, 100L, "e.txt", null)));
        List<FileInfo> result = provider.searchFiles(100L, null, 10);
        assertEquals(0, result.get(0).maxLineCount());
    }

    // ---------- searchChunks ----------

    @Test
    @DisplayName("searchChunks: 知识库不存在时返回空列表")
    void searchChunks_kbNotFound() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(null);
        assertEquals(List.of(), provider.searchChunks(100L, null, SearchType.VECTOR, "q", 10, 3));
    }

    @Test
    @DisplayName("searchChunks: esIndex 为空时返回空列表")
    void searchChunks_emptyEsIndex() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "  "));
        assertEquals(List.of(), provider.searchChunks(100L, null, SearchType.VECTOR, "q", 10, 3));
    }

    @Test
    @DisplayName("searchChunks: vectorModelId 缺失时抛 MODEL_NOT_FOUND")
    void searchChunks_vectorModelIdMissing() {
        KnowledgeBase k = kb(100L, "idx");
        k.setVectorModelId(null);
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(k);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> provider.searchChunks(100L, null, SearchType.VECTOR, "q", 10, 3));
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
        when(knowledgeSearchClient.vectorSearch(eq("idx"), eq(100L), eq(List.of(0.1f, 0.2f)), eq(5)))
                .thenReturn(List.of(chunk(100L, 2L, 5, "line5")));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(file(2L, 100L, "a.txt", "x"));
        when(knowledgeSearchClient.searchByFileAndLineRange(eq("idx"), eq(100L), eq(2L), anyInt(), anyInt()))
                .thenReturn(List.of(chunk(100L, 2L, 5, "line5")));

        List<TextChunkWithFile> result = provider.searchChunks(100L, null, SearchType.VECTOR, "q", 5, 3);

        assertEquals(1, result.size());
        TextChunkWithFile withFile = result.get(0);
        assertEquals(2L, withFile.fileId());
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
        when(knowledgeSearchClient.fullTextSearch("idx", 100L, "q", 5))
                .thenReturn(List.of(chunk(100L, 2L, 3, "line3")));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(file(2L, 100L, "a.txt", "x"));
        when(knowledgeSearchClient.searchByFileAndLineRange(eq("idx"), eq(100L), eq(2L), anyInt(), anyInt()))
                .thenReturn(List.of(chunk(100L, 2L, 3, "line3")));

        List<TextChunkWithFile> result = provider.searchChunks(100L, null, SearchType.FULLTEXT, "q", 5, 3);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).fileId());
        verify(knowledgeSearchClient, never()).vectorSearch(any(), any(), anyList(), anyInt());
    }

    @Test
    @DisplayName("searchChunks: fileId 过滤后为空时返回空列表")
    void searchChunks_fileIdFilterEmpty() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(knowledgeSearchClient.fullTextSearch("idx", 100L, "q", 5))
                .thenReturn(new java.util.ArrayList<>(List.of(chunk(100L, 2L, 3, "line3"))));

        List<TextChunkWithFile> result = provider.searchChunks(100L, 99L, SearchType.FULLTEXT, "q", 5, 3);
        assertEquals(List.of(), result);
        verify(knowledgeFileMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("searchChunks: 文件不存在时 fileName 使用 fileId 字符串")
    void searchChunks_fileNameFallback() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(knowledgeSearchClient.fullTextSearch("idx", 100L, "q", 5))
                .thenReturn(List.of(chunk(100L, 2L, 3, "line3")));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(null);
        when(knowledgeSearchClient.searchByFileAndLineRange(eq("idx"), eq(100L), eq(2L), anyInt(), anyInt()))
                .thenReturn(List.of(chunk(100L, 2L, 3, "line3")));

        List<TextChunkWithFile> result = provider.searchChunks(100L, null, SearchType.FULLTEXT, "q", 5, 3);
        assertEquals("2", result.get(0).fileName());
    }

    @Test
    @DisplayName("searchChunks: 上下文扩展按 lineNumber 去重")
    void searchChunks_contextDedup() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(knowledgeSearchClient.fullTextSearch("idx", 100L, "q", 5))
                .thenReturn(List.of(chunk(100L, 2L, 5, "line5")));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(file(2L, 100L, "a.txt", "x"));
        // 行范围 [max(1,5-3), 5+3] = [2, 8]，返回含重复行号 5 的邻居
        when(knowledgeSearchClient.searchByFileAndLineRange("idx", 100L, 2L, 2, 8))
                .thenReturn(List.of(
                        chunk(100L, 2L, 5, "line5"),
                        chunk(100L, 2L, 5, "line5-dupe"),
                        chunk(100L, 2L, 6, "line6")));

        List<TextChunkWithFile> result = provider.searchChunks(100L, null, SearchType.FULLTEXT, "q", 5, 3);
        List<TextChunkWithFile.TextChunk> chunks = result.get(0).chunkList();
        assertEquals(2, chunks.size());
        assertTrue(chunks.stream().allMatch(c -> c.lineNumber() == 5 || c.lineNumber() == 6));
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
        when(knowledgeSearchClient.vectorSearch(eq("idx"), eq(100L), anyList(), eq(5)))
                .thenReturn(List.of(chunk(100L, 2L, 5, "line5")));
        when(knowledgeSearchClient.fullTextSearch("idx", 100L, "q", 5))
                .thenReturn(List.of(chunk(100L, 2L, 5, "line5"), chunk(100L, 2L, 8, "line8")));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(file(2L, 100L, "a.txt", "x"));
        when(knowledgeSearchClient.searchByFileAndLineRange(eq("idx"), eq(100L), eq(2L), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    int start = invocation.getArgument(3);
                    int end = invocation.getArgument(4);
                    java.util.List<TextChunk> range = new java.util.ArrayList<>();
                    for (int ln = start; ln <= end; ln++) {
                        range.add(chunk(100L, 2L, ln, "line" + ln));
                    }
                    return range;
                });

        List<TextChunkWithFile> result = provider.searchChunks(100L, null, SearchType.HYBRID, "q", 5, 3);
        assertEquals(1, result.size());
        // 命中 line5(line 扩展 [2,8]) 与 line8(扩展 [5,11])，去重后行号 2..11
        assertEquals(10, result.get(0).chunkList().size());
    }

    @Test
    @DisplayName("searchChunks: 同一文件重叠/相邻行范围合并为单次查询")
    void searchChunks_mergedRanges() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(knowledgeSearchClient.fullTextSearch("idx", 100L, "q", 5))
                .thenReturn(new java.util.ArrayList<>(List.of(
                        chunk(100L, 2L, 5, "line5"),
                        chunk(100L, 2L, 7, "line7"))));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(file(2L, 100L, "a.txt", "x"));
        // line5 扩展 [2,8]，line7 扩展 [4,10]，重叠合并为 [2,10]，应只调用一次
        when(knowledgeSearchClient.searchByFileAndLineRange("idx", 100L, 2L, 2, 10))
                .thenReturn(List.of(chunk(100L, 2L, 5, "line5"), chunk(100L, 2L, 7, "line7")));

        List<TextChunkWithFile> result = provider.searchChunks(100L, null, SearchType.FULLTEXT, "q", 5, 3);

        assertEquals(1, result.size());
        verify(knowledgeSearchClient, times(1)).searchByFileAndLineRange(eq("idx"), eq(100L), eq(2L), anyInt(), anyInt());
    }

    @Test
    @DisplayName("searchChunks: 同一文件非相邻行范围保持多次查询")
    void searchChunks_nonAdjacentRanges() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(knowledgeSearchClient.fullTextSearch("idx", 100L, "q", 5))
                .thenReturn(new java.util.ArrayList<>(List.of(
                        chunk(100L, 2L, 5, "line5"),
                        chunk(100L, 2L, 100, "line100"))));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(file(2L, 100L, "a.txt", "x"));
        // line5 扩展 [2,8]，line100 扩展 [97,103]，不重叠应分别查询
        when(knowledgeSearchClient.searchByFileAndLineRange("idx", 100L, 2L, 2, 8))
                .thenReturn(List.of(chunk(100L, 2L, 5, "line5")));
        when(knowledgeSearchClient.searchByFileAndLineRange("idx", 100L, 2L, 97, 103))
                .thenReturn(List.of(chunk(100L, 2L, 100, "line100")));

        List<TextChunkWithFile> result = provider.searchChunks(100L, null, SearchType.FULLTEXT, "q", 5, 3);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).chunkList().size());
        verify(knowledgeSearchClient, times(2)).searchByFileAndLineRange(eq("idx"), eq(100L), eq(2L), anyInt(), anyInt());
    }

    // ---------- getFileChunks ----------

    @Test
    @DisplayName("getFileChunks: 知识库不存在或 esIndex 为空时返回 null")
    void getFileChunks_kbMissing() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(null);
        assertNull(provider.getFileChunks(100L, 2L, 0, 10));
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, ""));
        assertNull(provider.getFileChunks(100L, 2L, 0, 10));
        verify(knowledgeSearchClient, never()).searchByFileAndLineRange(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("getFileChunks: 正常返回文本块，fileName 取文件表")
    void getFileChunks_normal() {
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "idx"));
        when(knowledgeFileMapper.selectById(2L)).thenReturn(file(2L, 100L, "a.txt", "x"));
        when(knowledgeSearchClient.searchByFileAndLineRange("idx", 100L, 2L, 0, 10))
                .thenReturn(List.of(chunk(100L, 2L, 0, "line0"), chunk(100L, 2L, 1, "line1")));

        TextChunkWithFile result = provider.getFileChunks(100L, 2L, 0, 10);
        assertNotNull(result);
        assertEquals(2L, result.fileId());
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

        TextChunkWithFile result = provider.getFileChunks(100L, 2L, 0, 10);
        assertNotNull(result);
        assertEquals("2", result.fileName());
        assertEquals(List.of(), result.chunkList());
    }
}
