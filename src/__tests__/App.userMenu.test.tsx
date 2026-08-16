import { describe, it, expect, vi, beforeAll, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import App from '../App';

// 行为级测试：App Header 用户下拉菜单（角色区分）+ 修改显示名/修改密码 Modal 交互 + 退出登录。
// 仅 mock services/api，真实执行 services/auth（saveCurrentUser/logout）与 services/user（updateCurrentUser）逻辑。

const mockGet = vi.hoisted(() => vi.fn());
const mockPost = vi.hoisted(() => vi.fn());
const mockPut = vi.hoisted(() => vi.fn());

vi.mock('../services/api', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    put: mockPut,
  },
}));

const ADMIN_USER = { id: '1', loginName: 'admin', displayName: '管理员', userType: 2, enabled: 1 };
const NORMAL_USER = { id: '2', loginName: 'user', displayName: '普通用户', userType: 1, enabled: 1 };

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

beforeEach(() => {
  localStorage.clear();
  mockGet.mockReset();
  mockPost.mockReset();
  mockPut.mockReset();
  // /users 为分页对象；/models 系列接口返回数组（listModels/getPlatformConfig）
  mockGet.mockImplementation((url: string) => {
    const u = String(url);
    if (u.startsWith('/users')) {
      return Promise.resolve({
        data: { data: { list: [], total: 0, page: 1, size: 10 } },
      });
    }
    return Promise.resolve({ data: { data: [] } });
  });
  mockPost.mockResolvedValue({ data: { success: true, data: null } });
  // PUT /auth/me 返回「当前用户 + 更新字段」后的用户
  mockPut.mockImplementation((url: string, body: Record<string, unknown>) => {
    const current = JSON.parse(localStorage.getItem('currentUser') as string);
    return Promise.resolve({
      data: { data: { ...current, ...(body as object) } },
    });
  });
});

function seed(user: unknown): void {
  localStorage.setItem('currentUser', JSON.stringify(user));
}

function renderAt(path: string): void {
  render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  );
}

async function openUserMenu(displayName: string): Promise<void> {
  const trigger = screen.getByText(displayName);
  fireEvent.mouseEnter(trigger);
  await waitFor(() => {
    expect(document.querySelector('.ant-dropdown:not(.ant-dropdown-hidden)')).toBeTruthy();
  });
}

describe('App Header 用户菜单（角色区分）', () => {
  it('普通用户菜单包含 修改显示名/修改密码/退出 三项', async () => {
    seed(NORMAL_USER);
    renderAt('/models');
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '新增模型' })).toBeTruthy();
    });
    await openUserMenu('普通用户');
    const menu = document.querySelector('.ant-dropdown:not(.ant-dropdown-hidden)') as HTMLElement;
    expect(menu.textContent).toContain('修改显示名');
    expect(menu.textContent).toContain('修改密码');
    expect(menu.textContent).toContain('退出');
  });

  it('管理员菜单仅包含 修改密码/退出 两项（无修改显示名）', async () => {
    seed(ADMIN_USER);
    renderAt('/users');
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '添加用户' })).toBeTruthy();
    });
    await openUserMenu('管理员');
    const menu = document.querySelector('.ant-dropdown:not(.ant-dropdown-hidden)') as HTMLElement;
    expect(menu.textContent).toContain('修改密码');
    expect(menu.textContent).toContain('退出');
    expect(menu.textContent).not.toContain('修改显示名');
  });

  it('Header 右上角显示 Avatar 与当前用户显示名', async () => {
    seed(NORMAL_USER);
    renderAt('/models');
    await waitFor(() => {
      expect(screen.getByText('普通用户')).toBeTruthy();
    });
    expect(document.querySelector('.ant-avatar')).toBeTruthy();
  });
});

describe('App 修改显示名 Modal', () => {
  it('确认后调用 updateCurrentUser({displayName})，saveCurrentUser 同步 localStorage 并刷新 Header 显示、提示成功', async () => {
    seed(NORMAL_USER);
    renderAt('/models');
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '新增模型' })).toBeTruthy();
    });
    await openUserMenu('普通用户');
    const menu = document.querySelector('.ant-dropdown:not(.ant-dropdown-hidden)') as HTMLElement;
    fireEvent.click(Array.from(menu.querySelectorAll('li')).find((li) => li.textContent === '修改显示名') as HTMLElement);

    await waitFor(() => {
      expect(document.querySelector('.ant-modal')).toBeTruthy();
    });
    const modal = document.querySelector('.ant-modal') as HTMLElement;
    expect(modal.textContent).toContain('修改显示名');
    fireEvent.change(modal.querySelector('input[placeholder="请输入显示名"]') as HTMLInputElement, {
      target: { value: '改名后的我' },
    });
    fireEvent.click(Array.from(modal.querySelectorAll('button')).find((b) => b.textContent === 'OK') as HTMLElement);

    // updateCurrentUser 调用 PUT /auth/me 且仅传 displayName
    await waitFor(() => {
      expect(mockPut).toHaveBeenCalled();
    });
    expect(mockPut.mock.calls[0][0]).toBe('/auth/me');
    expect((mockPut.mock.calls[0] as [string, Record<string, unknown>])[1]).toEqual({
      displayName: '改名后的我',
    });

    // saveCurrentUser 同步 localStorage
    await waitFor(() => {
      const saved = JSON.parse(localStorage.getItem('currentUser') as string);
      expect(saved.displayName).toBe('改名后的我');
    });

    // 刷新 Header 显示 + 成功提示
    await waitFor(() => {
      expect(screen.getByText('改名后的我')).toBeTruthy();
    });
    await waitFor(() => {
      expect(screen.getByText('显示名修改成功')).toBeTruthy();
    });
  });
});

describe('App 修改密码 Modal', () => {
  it('确认后调用 updateCurrentUser({password})（不校验旧密码），提示成功', async () => {
    seed(ADMIN_USER);
    renderAt('/users');
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '添加用户' })).toBeTruthy();
    });
    await openUserMenu('管理员');
    const menu = document.querySelector('.ant-dropdown:not(.ant-dropdown-hidden)') as HTMLElement;
    fireEvent.click(Array.from(menu.querySelectorAll('li')).find((li) => li.textContent === '修改密码') as HTMLElement);

    await waitFor(() => {
      expect(document.querySelector('.ant-modal')).toBeTruthy();
    });
    const modal = document.querySelector('.ant-modal') as HTMLElement;
    expect(modal.textContent).toContain('修改密码');
    expect(modal.textContent).toContain('新密码');
    fireEvent.change(modal.querySelector('input[placeholder="请输入新密码"]') as HTMLInputElement, {
      target: { value: 'NewPass@123' },
    });
    fireEvent.click(Array.from(modal.querySelectorAll('button')).find((b) => b.textContent === 'OK') as HTMLElement);

    await waitFor(() => {
      expect(mockPut).toHaveBeenCalled();
    });
    expect(mockPut.mock.calls[0][0]).toBe('/auth/me');
    expect((mockPut.mock.calls[0] as [string, Record<string, unknown>])[1]).toEqual({
      password: 'NewPass@123',
    });
    await waitFor(() => {
      expect(screen.getByText('密码修改成功')).toBeTruthy();
    });
  });
});

describe('App 退出登录', () => {
  it('点击退出：调用 POST /api/auth/logout、清除 localStorage currentUser、跳转 /login', async () => {
    seed(NORMAL_USER);
    renderAt('/models');
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '新增模型' })).toBeTruthy();
    });
    await openUserMenu('普通用户');
    const menu = document.querySelector('.ant-dropdown:not(.ant-dropdown-hidden)') as HTMLElement;
    fireEvent.click(Array.from(menu.querySelectorAll('li')).find((li) => li.textContent === '退出') as HTMLElement);

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/auth/logout');
    });
    await waitFor(() => {
      expect(localStorage.getItem('currentUser')).toBeNull();
    });
    // 跳转 /login 渲染登录页
    await waitFor(() => {
      expect(screen.getByPlaceholderText('请输入登录名')).toBeTruthy();
    });
  });
});



