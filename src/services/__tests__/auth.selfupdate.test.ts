import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockPost = vi.hoisted(() => vi.fn());

vi.mock('../api', () => ({
  default: {
    post: mockPost,
  },
}));

import { saveCurrentUser, logout, getCurrentUser } from '../auth';
import type { User } from '../../types/user';

const USER: User = {
  id: '1',
  loginName: 'user',
  displayName: '普通用户',
  userType: 1,
  enabled: 1,
};

describe('saveCurrentUser（写入 localStorage currentUser）', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('应将用户信息 JSON 序列化后写入 localStorage currentUser', () => {
    saveCurrentUser(USER);
    expect(localStorage.getItem('currentUser')).toBe(JSON.stringify(USER));
  });

  it('写入后可被 getCurrentUser 读回', () => {
    saveCurrentUser(USER);
    expect(getCurrentUser()).toEqual(USER);
  });

  it('应覆盖旧值（更新显示名后 localStorage 同步为新用户信息）', () => {
    saveCurrentUser(USER);
    const updated = { ...USER, displayName: '新显示名' };
    saveCurrentUser(updated);
    expect(JSON.parse(localStorage.getItem('currentUser') as string).displayName).toBe('新显示名');
  });
});

describe('logout（POST /api/auth/logout 并清除本地登录状态）', () => {
  beforeEach(() => {
    localStorage.clear();
    mockPost.mockReset();
    saveCurrentUser(USER);
  });

  it('接口成功时：调用 POST /auth/logout 并清除 localStorage currentUser', async () => {
    mockPost.mockResolvedValueOnce({ data: { success: true, data: null } });
    await logout();
    expect(mockPost).toHaveBeenCalledWith('/auth/logout');
    expect(localStorage.getItem('currentUser')).toBeNull();
  });

  it('接口失败时：仍清除 localStorage currentUser（finally 兜底）', async () => {
    mockPost.mockRejectedValueOnce(new Error('Network Error'));
    await expect(logout()).rejects.toThrow('Network Error');
    expect(localStorage.getItem('currentUser')).toBeNull();
  });

  it('接口失败时向上抛出异常但本地状态已清除（调用方无需阻塞跳转）', async () => {
    mockPost.mockRejectedValueOnce(new Error('Network Error'));
    let thrown = false;
    try {
      await logout();
    } catch {
      thrown = true;
    }
    expect(thrown).toBe(true);
    expect(getCurrentUser()).toBeNull();
  });
});
