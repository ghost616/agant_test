import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import App from '../App';

// 行为级补充测试（E2E 被沙箱阻断时的替代验证）：
// 直接渲染 <App /> 验证 4 个功能点：守卫重定向、按角色落地页、菜单角色过滤、登录页独立全屏。

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn((url: string) => {
      if (String(url).startsWith('/users')) {
        return Promise.resolve({ data: { data: { list: [], total: 0, page: 1, size: 10 } } });
      }
      return Promise.resolve({ data: { data: [] } });
    }),
    post: vi.fn(() => Promise.resolve({ data: { data: null } })),
    put: vi.fn(() => Promise.resolve({ data: { data: null } })),
    delete: vi.fn(() => Promise.resolve({ data: { data: null } })),
  },
}));

beforeAll(() => {
  window.matchMedia =
    window.matchMedia ||
    ((query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }));
  if (!window.ResizeObserver) {
    (window as unknown as { ResizeObserver: unknown }).ResizeObserver = class {
      observe() {}
      unobserve() {}
      disconnect() {}
    };
  }
});

const ADMIN_USER = { id: '1', loginName: 'admin', displayName: '管理员', userType: 2, enabled: 1 };
const NORMAL_USER = { id: '2', loginName: 'user', displayName: '普通用户', userType: 1, enabled: 1 };

function seedUser(user: unknown): void {
  localStorage.setItem('currentUser', JSON.stringify(user));
}

function menuText(): string {
  const menu = document.querySelector('.ant-menu');
  return menu ? (menu.textContent ?? '') : '';
}

describe('App 登录守卫与菜单权限（行为级验证）', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('未登录访问 /models：守卫重定向到登录页，且登录页为独立全屏页面（无侧边栏菜单）', async () => {
    render(
      <MemoryRouter initialEntries={['/models']}>
        <App />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByPlaceholderText('请输入登录名')).toBeTruthy();
    });
    expect(screen.getByRole('button', { name: /登\s*录/ })).toBeTruthy();
    expect(document.querySelector('.ant-layout-sider')).toBeNull();
  });

  it('管理员（userType=2）登录后根路径 / 落地 /users，用户管理菜单可见', async () => {
    seedUser(ADMIN_USER);
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '添加用户' })).toBeTruthy();
    });
    const menu = menuText();
    expect(menu).toContain('用户管理');
    expect(menu).toContain('模型管理');
  });

  it('普通用户（userType=1）登录后根路径 / 落地 /models，用户管理菜单不可见', async () => {
    seedUser(NORMAL_USER);
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '新增模型' })).toBeTruthy();
    });
    const menu = menuText();
    expect(menu).toContain('模型管理');
    expect(menu).not.toContain('用户管理');
  });
});
