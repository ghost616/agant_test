package com.ghost616.platform.service.knowledge;

import com.ghost616.agentbase.dto.model.EmbeddingRequest;
import com.ghost616.agentbase.dto.model.EmbeddingResponse;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import com.ghost616.platform.entity.KnowledgeBase;
import com.ghost616.platform.entity.KnowledgeFile;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.enums.PublishStatus;
import com.ghost616.platform.model.TextChunk;
import com.ghost616.platform.repository.KnowledgeBaseMapper;
import com.ghost616.platform.repository.KnowledgeFileMapper;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.service.search.KnowledgeSearchClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgePublishServiceTest {

    @Mock
    private KnowledgeFileMapper knowledgeFileMapper;
    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;
    @Mock
    private ModelConfigMapper modelConfigMapper;
    @Mock
    private ModelInvokerManager modelInvokerManager;
    @Mock
    private ModelInvoker modelInvoker;
    @Mock
    private KnowledgeSearchClient knowledgeSearchClient;

    private KnowledgePublishService service;

    private KnowledgePublishService newService() {
        service = new KnowledgePublishService(
                knowledgeFileMapper, knowledgeBaseMapper, modelConfigMapper,
                modelInvokerManager, knowledgeSearchClient);
        return service;
    }

    private KnowledgeFile file(Long id, Long kbId, String content, PublishStatus publishStatus) {
        KnowledgeFile f = new KnowledgeFile();
        f.setId(id);
        f.setKnowledgeBaseId(kbId);
        f.setFileName("f" + id + ".txt");
        f.setFileContent(content);
        f.setPublishStatus(publishStatus);
        f.setStatus(CommonStatus.ENABLED);
        return f;
    }

    private KnowledgeBase kb(Long id, String esIndex) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setName("kb-" + id);
        kb.setVectorModelId(5L);
        kb.setEsIndex(esIndex);
        kb.setStatus(CommonStatus.ENABLED);
        return kb;
    }

    private ModelConfig model(Long id) {
        ModelConfig m = new ModelConfig();
        m.setId(id);
        m.setModelName("embed-model");
        m.setRequestType("completions");
        return m;
    }

    private EmbeddingResponse embeddingResponse(float... values) {
        List<Float> vector = new java.util.ArrayList<>();
        for (float v : values) {
            vector.add(v);
        }
        return EmbeddingResponse.builder()
                .embeddings(List.of(EmbeddingResponse.EmbeddingItem.builder()
                        .embedding(vector)
                        .build()))
                .build();
    }

    private void stubPublishDependencies() {
        when(modelConfigMapper.selectById(5L)).thenReturn(model(5L));
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);
    }

    @Test
    void publishFile_成功发布并写入索引() {
        newService();
        KnowledgeFile f = file(1L, 100L, "line1\n\nline2\n", PublishStatus.UNPUBLISHED);
        when(knowledgeFileMapper.selectById(1L)).thenReturn(f);
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "agent_idx"));
        stubPublishDependencies();
        when(modelInvoker.embed(any(EmbeddingRequest.class))).thenReturn(embeddingResponse(0.1f, 0.2f));
        when(knowledgeSearchClient.indexExists("agent_idx")).thenReturn(true);

        service.publishFile(1L);

        verify(knowledgeSearchClient).deleteByFile("agent_idx", 1L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TextChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(knowledgeSearchClient).batchSave(eq("agent_idx"), captor.capture());
        List<TextChunk> chunks = captor.getValue();
        assertEquals(2, chunks.size(), "空行应被过滤");
        assertEquals("line1", chunks.get(0).getText());
        assertEquals(Integer.valueOf(1), chunks.get(0).getLineNumber());
        assertEquals("line2", chunks.get(1).getText());
        assertEquals(Integer.valueOf(2), chunks.get(1).getLineNumber());
        assertEquals(List.of(0.1f, 0.2f), chunks.get(0).getVector());
        assertEquals(Boolean.TRUE, chunks.get(0).getKbEnabled());
        assertEquals(Boolean.TRUE, chunks.get(0).getFileEnabled());
        assertEquals(PublishStatus.PUBLISHED, f.getPublishStatus());
        verify(knowledgeSearchClient, never()).createIndex(anyString(), anyInt());
    }

    @Test
    void publishFile_索引不存在时按向量维度创建索引() {
        newService();
        KnowledgeFile f = file(1L, 100L, "line1", PublishStatus.UNPUBLISHED);
        when(knowledgeFileMapper.selectById(1L)).thenReturn(f);
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "agent_idx"));
        stubPublishDependencies();
        when(modelInvoker.embed(any(EmbeddingRequest.class))).thenReturn(embeddingResponse(0.1f, 0.2f));
        when(knowledgeSearchClient.indexExists("agent_idx")).thenReturn(false);

        service.publishFile(1L);

        verify(knowledgeSearchClient, never()).deleteByFile("agent_idx", 1L);
        verify(knowledgeSearchClient).createIndex("agent_idx", 2);
        verify(knowledgeSearchClient).batchSave(eq("agent_idx"), anyList());
        assertEquals(PublishStatus.PUBLISHED, f.getPublishStatus());
    }

    @Test
    void publishFile_失败置发布错误并清理索引() {
        newService();
        KnowledgeFile f = file(1L, 100L, "line1", PublishStatus.UNPUBLISHED);
        when(knowledgeFileMapper.selectById(1L)).thenReturn(f);
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "agent_idx"));
        stubPublishDependencies();
        when(modelInvoker.embed(any(EmbeddingRequest.class))).thenThrow(new RuntimeException("embed 失败"));
        when(knowledgeSearchClient.indexExists("agent_idx")).thenReturn(true);

        service.publishFile(1L);

        assertEquals(PublishStatus.PUBLISH_ERROR, f.getPublishStatus());
        verify(knowledgeSearchClient, times(2)).deleteByFile("agent_idx", 1L);
        verify(knowledgeSearchClient, never()).batchSave(anyString(), anyList());
    }

    @Test
    void publishFile_文件不存在时不处理() {
        newService();
        when(knowledgeFileMapper.selectById(1L)).thenReturn(null);

        service.publishFile(1L);

        verify(knowledgeFileMapper, never()).updateById(any(KnowledgeFile.class));
        verify(knowledgeBaseMapper, never()).selectById(any());
    }

    @Test
    void publishFile_知识库无索引置发布错误() {
        newService();
        KnowledgeFile f = file(1L, 100L, "content", PublishStatus.UNPUBLISHED);
        when(knowledgeFileMapper.selectById(1L)).thenReturn(f);
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, null));

        service.publishFile(1L);

        assertEquals(PublishStatus.PUBLISH_ERROR, f.getPublishStatus());
        verify(knowledgeSearchClient, never()).batchSave(anyString(), anyList());
    }

    @Test
    void publishFile_内容为空时仍标记已发布() {
        newService();
        KnowledgeFile f = file(1L, 100L, null, PublishStatus.UNPUBLISHED);
        when(knowledgeFileMapper.selectById(1L)).thenReturn(f);
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb(100L, "agent_idx"));
        stubPublishDependencies();
        when(knowledgeSearchClient.indexExists("agent_idx")).thenReturn(true);

        service.publishFile(1L);

        verify(knowledgeSearchClient).batchSave(eq("agent_idx"), anyList());
        assertEquals(PublishStatus.PUBLISHED, f.getPublishStatus());
    }

    @Test
    void rebuildKnowledgeBase_删除索引并重新发布全部已发布文件() {
        newService();
        KnowledgeBase kb = kb(100L, "agent_idx");
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb);
        KnowledgeFile f1 = file(1L, 100L, "l1", PublishStatus.PUBLISHED);
        KnowledgeFile f2 = file(2L, 100L, "l2", PublishStatus.PENDING_PUBLISH);
        when(knowledgeFileMapper.selectById(1L)).thenReturn(f1);
        when(knowledgeFileMapper.selectById(2L)).thenReturn(f2);
        when(knowledgeFileMapper.selectList(any())).thenReturn(List.of(f1, f2));
        stubPublishDependencies();
        when(modelInvoker.embed(any(EmbeddingRequest.class))).thenReturn(embeddingResponse(0.1f));
        when(knowledgeSearchClient.indexExists("agent_idx")).thenReturn(true);

        List<Boolean> rebuildingValues = new ArrayList<>();
        doAnswer(invocation -> {
            rebuildingValues.add(((KnowledgeBase) invocation.getArgument(0)).getRebuilding());
            return 0;
        }).when(knowledgeBaseMapper).updateById(any(KnowledgeBase.class));

        service.rebuildKnowledgeBase(100L);

        verify(knowledgeSearchClient).deleteIndex("agent_idx");
        verify(knowledgeSearchClient).deleteByFile("agent_idx", 1L);
        verify(knowledgeSearchClient).deleteByFile("agent_idx", 2L);
        verify(knowledgeSearchClient, atLeast(2)).batchSave(eq("agent_idx"), anyList());
        assertEquals(PublishStatus.PUBLISHED, f1.getPublishStatus());
        assertEquals(PublishStatus.PUBLISHED, f2.getPublishStatus());

        assertEquals(Boolean.TRUE, rebuildingValues.get(0), "重建开始时 rebuilding 应为 true");
        assertEquals(Boolean.FALSE, rebuildingValues.get(rebuildingValues.size() - 1), "重建完成后 rebuilding 应为 false");
    }

    @Test
    void rebuildKnowledgeBase_知识库不存在抛异常() {
        newService();
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.rebuildKnowledgeBase(100L));
        assertEquals(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void rebuildKnowledgeBase_异常时重置rebuilding状态() {
        newService();
        KnowledgeBase kb = kb(100L, "agent_idx");
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb);
        doThrow(new IllegalStateException("es 错误")).when(knowledgeSearchClient).deleteIndex("agent_idx");

        assertThrows(IllegalStateException.class, () -> service.rebuildKnowledgeBase(100L));

        ArgumentCaptor<KnowledgeBase> kbCaptor = ArgumentCaptor.forClass(KnowledgeBase.class);
        verify(knowledgeBaseMapper, atLeast(2)).updateById(kbCaptor.capture());
        List<KnowledgeBase> updates = kbCaptor.getAllValues();
        assertEquals(Boolean.FALSE, updates.get(updates.size() - 1).getRebuilding(), "异常后 rebuilding 应复位为 false");
    }
}
