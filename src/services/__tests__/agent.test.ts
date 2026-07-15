import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockGet = vi.hoisted(() => vi.fn());
const mockPost = vi.hoisted(() => vi.fn());
const mockPut = vi.hoisted(() => vi.fn());
const mockDelete = vi.hoisted(() => vi.fn());

vi.mock('../api', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    put: mockPut,
    delete: mockDelete,
  },
}));

import {
  listAgents,
  getAgent,
  createAgent,
  updateAgent,
  deleteAgent,
  updateAgentStatus,
} from '../agent';

describe('listAgents', () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it('应调用 GET /agents 并返回 AgentConfig 列表', async () => {
    const fakeData = [
      { id: '1', name: 'agent-a', tools: [], skills: [], status: 'ENABLED' },
    ];
    mockGet.mockResolvedValueOnce({ data: { data: fakeData } });
    const result = await listAgents();
    expect(mockGet).toHaveBeenCalledWith('/agents', { params: undefined });
    expect(result).toEqual(fakeData);
  });

  it('应传递 name 和 status 参数', async () => {
    mockGet.mockResolvedValueOnce({ data: { data: [] } });
    await listAgents({ name: 'test', status: 'ENABLED' });
    expect(mockGet).toHaveBeenCalledWith('/agents', {
      params: { name: 'test', status: 'ENABLED' },
    });
  });

  it('应在 API 失败时抛出错误', async () => {
    mockGet.mockRejectedValueOnce(new Error('Network Error'));
    await expect(listAgents()).rejects.toThrow('Network Error');
  });
});

describe('getAgent', () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it('应调用 GET /agents/{id} 并返回 AgentConfig', async () => {
    const fakeAgent = { id: 'agent-1', name: 'test', tools: [], skills: [], status: 'ENABLED' };
    mockGet.mockResolvedValueOnce({ data: { data: fakeAgent } });
    const result = await getAgent('agent-1');
    expect(mockGet).toHaveBeenCalledWith('/agents/agent-1');
    expect(result).toEqual(fakeAgent);
  });

  it('应正确处理不同 id', async () => {
    mockGet.mockResolvedValueOnce({ data: { data: null } });
    await getAgent('id-a');
    expect(mockGet).toHaveBeenCalledWith('/agents/id-a');

    mockGet.mockResolvedValueOnce({ data: { data: null } });
    await getAgent('id-b');
    expect(mockGet).toHaveBeenCalledWith('/agents/id-b');
  });
});

describe('createAgent', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('应调用 POST /agents 并返回新建 AgentConfig', async () => {
    const newAgent = { id: 'new-1', name: 'new-agent', tools: [], skills: [], status: 'ENABLED' };
    const formData = { name: 'new-agent', tools: [{ toolId: 't1', sessionAuth: 'ALL' as const }] };
    mockPost.mockResolvedValueOnce({ data: { data: newAgent } });
    const result = await createAgent(formData);
    expect(mockPost).toHaveBeenCalledWith('/agents', formData);
    expect(result).toEqual(newAgent);
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPost.mockRejectedValueOnce(new Error('Network Error'));
    await expect(createAgent({ name: 'test' })).rejects.toThrow('Network Error');
  });
});

describe('updateAgent', () => {
  beforeEach(() => {
    mockPut.mockReset();
  });

  it('应调用 PUT /agents/{id} 并返回更新后的 AgentConfig', async () => {
    const updated = { id: 'a1', name: 'updated', tools: [], skills: [], status: 'ENABLED' };
    const formData = { name: 'updated', tools: [{ toolId: 't1', sessionAuth: 'ALL' as const }] };
    mockPut.mockResolvedValueOnce({ data: { data: updated } });
    const result = await updateAgent('a1', formData);
    expect(mockPut).toHaveBeenCalledWith('/agents/a1', formData);
    expect(result).toEqual(updated);
  });
});

describe('deleteAgent', () => {
  beforeEach(() => {
    mockDelete.mockReset();
  });

  it('应调用 DELETE /agents/{id}', async () => {
    mockDelete.mockResolvedValueOnce(undefined);
    await deleteAgent('agent-1');
    expect(mockDelete).toHaveBeenCalledWith('/agents/agent-1');
  });
});

describe('updateAgentStatus', () => {
  beforeEach(() => {
    mockPut.mockReset();
  });

  it('应调用 PUT /agents/{id}/status', async () => {
    mockPut.mockResolvedValueOnce(undefined);
    await updateAgentStatus('agent-1', 'ENABLED');
    expect(mockPut).toHaveBeenCalledWith('/agents/agent-1/status', { status: 'ENABLED' });
  });
});
