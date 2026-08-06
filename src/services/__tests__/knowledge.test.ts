import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockGet = vi.hoisted(() => vi.fn());
const mockPut = vi.hoisted(() => vi.fn());
const mockPost = vi.hoisted(() => vi.fn());

vi.mock('../api', () => ({
  default: {
    get: mockGet,
    put: mockPut,
    post: mockPost,
  },
}));

import {
  getKnowledgeFileContent,
  publishKnowledgeFile,
  rebuildKnowledgeBaseES,
  refreshKnowledgeFiles,
  updateKnowledgeFileContent,
} from '../knowledge';

describe('getKnowledgeFileContent', () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it('应调用 GET /knowledge-bases/{kbId}/files/{id}/content', async () => {
    mockGet.mockResolvedValueOnce({ data: { success: true, data: '文件内容' } });
    await getKnowledgeFileContent('kb-1', 'file-1');
    expect(mockGet).toHaveBeenCalledWith(
      '/knowledge-bases/kb-1/files/file-1/content',
    );
  });

  it('应返回 ApiResponse.data 作为内容字符串', async () => {
    const content = '# 一级标题\n\n正文';
    mockGet.mockResolvedValueOnce({ data: { success: true, data: content } });
    const result = await getKnowledgeFileContent('kb-1', 'file-1');
    expect(result).toBe(content);
  });

  it('应返回空字符串内容', async () => {
    mockGet.mockResolvedValueOnce({ data: { success: true, data: '' } });
    const result = await getKnowledgeFileContent('kb-1', 'file-1');
    expect(result).toBe('');
  });

  it('应在 API 失败时抛出错误', async () => {
    mockGet.mockRejectedValueOnce(new Error('Network Error'));
    await expect(getKnowledgeFileContent('kb-1', 'file-1')).rejects.toThrow(
      'Network Error',
    );
  });
});

describe('updateKnowledgeFileContent', () => {
  beforeEach(() => {
    mockPut.mockReset();
  });

  it('应调用 PUT /knowledge-bases/{kbId}/files/{id}/content', async () => {
    mockPut.mockResolvedValueOnce({ data: { success: true, data: null } });
    await updateKnowledgeFileContent('kb-1', 'file-1', '新内容');
    expect(mockPut).toHaveBeenCalledWith(
      '/knowledge-bases/kb-1/files/file-1/content',
      '新内容',
      { headers: { 'Content-Type': 'text/plain' } },
    );
  });

  it('请求体应为原始字符串且 Content-Type 为 text/plain（与后端 consumes 对齐）', async () => {
    mockPut.mockResolvedValueOnce({ data: { success: true, data: null } });
    const content = '# 标题\n\n这是 **加粗** 内容';
    await updateKnowledgeFileContent('kb-1', 'file-1', content);
    const [url, body, config] = mockPut.mock.calls[0];
    expect(url).toBe('/knowledge-bases/kb-1/files/file-1/content');
    expect(body).toBe(content);
    expect(typeof body).toBe('string');
    expect(config).toEqual({ headers: { 'Content-Type': 'text/plain' } });
  });

  it('应正确处理不同 kbId/id 参数', async () => {
    mockPut.mockResolvedValueOnce({ data: { success: true, data: null } });
    await updateKnowledgeFileContent('kb-a', 'file-b', '内容');
    expect(mockPut).toHaveBeenCalledWith(
      '/knowledge-bases/kb-a/files/file-b/content',
      '内容',
      { headers: { 'Content-Type': 'text/plain' } },
    );
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPut.mockRejectedValueOnce(new Error('Network Error'));
    await expect(
      updateKnowledgeFileContent('kb-1', 'file-1', '内容'),
    ).rejects.toThrow('Network Error');
  });
});

describe('publishKnowledgeFile', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('应调用 POST /knowledge-bases/{kbId}/files/{fileId}/publish', async () => {
    mockPost.mockResolvedValueOnce({ data: { success: true, data: null } });
    await publishKnowledgeFile('kb-1', 'file-1');
    expect(mockPost).toHaveBeenCalledWith('/knowledge-bases/kb-1/files/file-1/publish');
  });

  it('应正确处理不同 kbId/fileId 参数', async () => {
    mockPost.mockResolvedValueOnce({ data: { success: true, data: null } });
    await publishKnowledgeFile('kb-a', 'file-b');
    expect(mockPost).toHaveBeenCalledWith('/knowledge-bases/kb-a/files/file-b/publish');
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPost.mockRejectedValueOnce(new Error('发布失败'));
    await expect(publishKnowledgeFile('kb-1', 'file-1')).rejects.toThrow('发布失败');
  });
});

describe('refreshKnowledgeFiles', () => {
  beforeEach(() => {
    mockPut.mockReset();
  });

  it('应调用 PUT /knowledge-bases/{kbId}/files/refresh', async () => {
    mockPut.mockResolvedValueOnce({ data: { success: true, data: null } });
    await refreshKnowledgeFiles('kb-1');
    expect(mockPut).toHaveBeenCalledWith('/knowledge-bases/kb-1/files/refresh');
  });

  it('应正确处理不同 kbId 参数', async () => {
    mockPut.mockResolvedValueOnce({ data: { success: true, data: null } });
    await refreshKnowledgeFiles('kb-x');
    expect(mockPut).toHaveBeenCalledWith('/knowledge-bases/kb-x/files/refresh');
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPut.mockRejectedValueOnce(new Error('刷新失败'));
    await expect(refreshKnowledgeFiles('kb-1')).rejects.toThrow('刷新失败');
  });
});

describe('rebuildKnowledgeBaseES', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('应调用 POST /knowledge-bases/{kbId}/rebuild-es', async () => {
    mockPost.mockResolvedValueOnce({ data: { success: true, data: null } });
    await rebuildKnowledgeBaseES('kb-1');
    expect(mockPost).toHaveBeenCalledWith('/knowledge-bases/kb-1/rebuild-es');
  });

  it('应正确处理不同 kbId 参数', async () => {
    mockPost.mockResolvedValueOnce({ data: { success: true, data: null } });
    await rebuildKnowledgeBaseES('kb-y');
    expect(mockPost).toHaveBeenCalledWith('/knowledge-bases/kb-y/rebuild-es');
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPost.mockRejectedValueOnce(new Error('重构失败'));
    await expect(rebuildKnowledgeBaseES('kb-1')).rejects.toThrow('重构失败');
  });
});
