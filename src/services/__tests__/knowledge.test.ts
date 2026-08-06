import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockGet = vi.hoisted(() => vi.fn());
const mockPut = vi.hoisted(() => vi.fn());

vi.mock('../api', () => ({
  default: {
    get: mockGet,
    put: mockPut,
  },
}));

import {
  getKnowledgeFileContent,
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
