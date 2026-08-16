import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockPut = vi.hoisted(() => vi.fn());

vi.mock('../api', () => ({
  default: {
    put: mockPut,
  },
}));

import { updateCurrentUser } from '../user';
import type { User } from '../../types/user';

const UPDATED_USER: User = {
  id: '1',
  loginName: 'user',
  displayName: '新显示名',
  userType: 1,
  enabled: 1,
};

describe('updateCurrentUser（PUT /api/auth/me 自助修改）', () => {
  beforeEach(() => {
    mockPut.mockReset();
  });

  it('应调用 PUT /auth/me 并传递 UserSelfUpdateRequest（仅 displayName）', async () => {
    mockPut.mockResolvedValueOnce({ data: { data: UPDATED_USER } });
    const result = await updateCurrentUser({ displayName: '新显示名' });
    expect(mockPut).toHaveBeenCalledWith('/auth/me', { displayName: '新显示名' });
    expect(result).toEqual(UPDATED_USER);
  });

  it('应调用 PUT /auth/me 并传递 UserSelfUpdateRequest（仅 password）', async () => {
    mockPut.mockResolvedValueOnce({ data: { data: UPDATED_USER } });
    await updateCurrentUser({ password: 'new-pwd' });
    expect(mockPut).toHaveBeenCalledWith('/auth/me', { password: 'new-pwd' });
  });

  it('应调用 PUT /auth/me 并传递空对象（无可修改字段时不传多余字段）', async () => {
    mockPut.mockResolvedValueOnce({ data: { data: UPDATED_USER } });
    await updateCurrentUser({});
    expect(mockPut).toHaveBeenCalledWith('/auth/me', {});
  });

  it('应返回更新后的 User（res.data.data）', async () => {
    mockPut.mockResolvedValueOnce({ data: { data: UPDATED_USER } });
    const result = await updateCurrentUser({ displayName: 'x' });
    expect(result.loginName).toBe('user');
    expect(result.displayName).toBe('新显示名');
  });

  it('应在 API 失败时抛出错误（不吞异常）', async () => {
    mockPut.mockRejectedValueOnce(new Error('Network Error'));
    await expect(updateCurrentUser({ displayName: 'x' })).rejects.toThrow('Network Error');
  });

  it('不应包含 enabled 字段（enabled 不可自助修改）', async () => {
    mockPut.mockResolvedValueOnce({ data: { data: UPDATED_USER } });
    await updateCurrentUser({ displayName: 'x', password: 'p' } as never);
    const [, body] = mockPut.mock.calls[0] as [string, Record<string, unknown>];
    expect(body).not.toHaveProperty('enabled');
  });
});
