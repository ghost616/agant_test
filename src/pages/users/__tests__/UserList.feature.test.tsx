import { describe, it, expect, vi, beforeAll, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent, cleanup } from '@testing-library/react';
import UserList from '../UserList';

// 行为级测试：验证 UserList 布局（按钮右置/无用户类型列）、
// 添加/修改 Modal 结构（无允许登录 Switch）、提交 payload 不含 enabled。

const mockGet = vi.hoisted(() => vi.fn());
const mockPost = vi.hoisted(() => vi.fn());
const mockPut = vi.hoisted(() => vi.fn());

vi.mock('../../../services/api', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    put: mockPut,
  },
}));

const ADMIN_USER = { id: '1', loginName: 'admin', displayName: '管理员', userType: 2, enabled: 1 };
const NORMAL_RECORD = {
  id: '2',
  loginName: 'normal',
  displayName: '普通用户',
  userType: 1,
  enabled: 1,
  createTime: '2026-08-16T00:00:00',
};

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
  mockGet.mockResolvedValue({
    data: { data: { list: [NORMAL_RECORD], total: 1, page: 1, size: 10 } },
  });
  mockPost.mockResolvedValue({ data: { data: NORMAL_RECORD } });
  mockPut.mockResolvedValue({ data: { data: NORMAL_RECORD } });
  localStorage.setItem('currentUser', JSON.stringify(ADMIN_USER));
});

function openModalByButton(name: string): void {
  fireEvent.click(screen.getByRole('button', { name }));
}

describe('UserList 布局与表单（用户管理前端改造）', () => {
  it('非管理员访问显示 403 无权限，不渲染表格', async () => {
    localStorage.setItem('currentUser', JSON.stringify({ ...ADMIN_USER, userType: 1 }));
    render(<UserList />);
    await waitFor(() => {
      expect(screen.getByText('无权限访问')).toBeTruthy();
    });
    expect(screen.queryByText('用户管理')).toBeNull();
  });

  it('页面头部 flex/justify-between：标题「用户管理」在左、「添加用户」按钮在右', async () => {
    render(<UserList />);
    await waitFor(() => {
      expect(screen.getByText('用户管理')).toBeTruthy();
    });
    const title = screen.getByText('用户管理');
    const btn = screen.getByRole('button', { name: '添加用户' });
    // 二者位于同一个 flex 容器中（antd Typography.Title level=4）
    const header = title.closest('div');
    expect(header).toBeTruthy();
    expect(header!.contains(btn)).toBe(true);
    expect(header!.style.display).toBe('flex');
    expect(header!.style.justifyContent).toBe('space-between');
    expect(header!.style.alignItems).toBe('center');
  });

  it('表格无「用户类型」列（仅登录名/显示名/登录状态/创建时间/操作）', async () => {
    render(<UserList />);
    await waitFor(() => {
      expect(screen.getByText('普通用户')).toBeTruthy();
    });
    const headers = Array.from(document.querySelectorAll('.ant-table-thead th')).map(
      (th) => th.textContent,
    );
    expect(headers).toContain('登录名');
    expect(headers).toContain('显示名');
    expect(headers).toContain('登录状态');
    expect(headers).toContain('创建时间');
    expect(headers).toContain('操作');
    expect(headers.join(',')).not.toContain('用户类型');
  });

  it('操作列保留「修改」与「禁止登录/恢复登录」按钮', async () => {
    render(<UserList />);
    await waitFor(() => {
      expect(screen.getByText('普通用户')).toBeTruthy();
    });
    const row = document.querySelector('tr.ant-table-row');
    expect(row).toBeTruthy();
    expect(row!.textContent).toContain('修改');
    expect(row!.textContent).toContain('禁止登录'); // enabled=1 显示禁止登录
  });

  it('添加用户 Modal：仅登录名(必填)/显示名/密码(必填)，无「允许登录」Switch', async () => {
    render(<UserList />);
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '添加用户' })).toBeTruthy();
    });
    openModalByButton('添加用户');
    await waitFor(() => {
      expect(document.querySelector('.ant-modal')).toBeTruthy();
    });
    const modal = document.querySelector('.ant-modal') as HTMLElement;
    expect(modal.querySelector('input[placeholder="请输入登录名"]')).toBeTruthy();
    expect(modal.querySelector('input[placeholder="请输入显示名"]')).toBeTruthy();
    expect(modal.querySelector('input[placeholder="请输入密码"]')).toBeTruthy();
    // 无允许登录 Switch（antd Switch 为 input[type=checkbox] 或 button.ant-switch）
    expect(modal.querySelector('.ant-switch')).toBeNull();
    expect(modal.textContent).not.toContain('允许登录');
  });

  it('添加用户提交 payload 不含 enabled，调用 POST /users', async () => {
    render(<UserList />);
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '添加用户' })).toBeTruthy();
    });
    openModalByButton('添加用户');
    await waitFor(() => {
      expect(document.querySelector('.ant-modal input[placeholder="请输入登录名"]')).toBeTruthy();
    });
    const modal = document.querySelector('.ant-modal') as HTMLElement;
    fireEvent.change(modal.querySelector('input[placeholder="请输入登录名"]') as HTMLInputElement, {
      target: { value: 'newbie' },
    });
    fireEvent.change(modal.querySelector('input[placeholder="请输入显示名"]') as HTMLInputElement, {
      target: { value: '新用户' },
    });
    fireEvent.change(modal.querySelector('input[placeholder="请输入密码"]') as HTMLInputElement, {
      target: { value: 'P@ss123' },
    });
    fireEvent.click(Array.from(modal.querySelectorAll('button')).find((b) => b.textContent === 'OK') as HTMLElement);
    await waitFor(() => {
      expect(mockPost).toHaveBeenCalled();
    });
    const [url, body] = mockPost.mock.calls[0] as [string, Record<string, unknown>];
    expect(url).toBe('/users');
    expect(body).toEqual({ loginName: 'newbie', displayName: '新用户', password: 'P@ss123' });
    expect(body).not.toHaveProperty('enabled');
  });

  it('修改用户 Modal：仅显示名/密码（密码留空不修改），无登录名、无「允许登录」Switch', async () => {
    render(<UserList />);
    await waitFor(() => {
      expect(screen.getByText('普通用户')).toBeTruthy();
    });
    const row = document.querySelector('tr.ant-table-row') as HTMLElement;
    fireEvent.click(Array.from(row.querySelectorAll('button')).find((b) => b.textContent === '修改') as HTMLElement);
    await waitFor(() => {
      expect(document.querySelector('.ant-modal')).toBeTruthy();
    });
    const modal = document.querySelector('.ant-modal') as HTMLElement;
    expect(modal.textContent).toContain('修改用户');
    expect(modal.querySelector('input[placeholder="请输入显示名"]')).toBeTruthy();
    expect(modal.querySelector('input[placeholder="留空则不修改密码"]')).toBeTruthy();
    // 无登录名输入框
    expect(modal.querySelector('input[placeholder="请输入登录名"]')).toBeNull();
    expect(modal.querySelector('.ant-switch')).toBeNull();
    expect(modal.textContent).not.toContain('允许登录');
  });

  it('修改用户提交 payload 不含 enabled，密码留空时不传 password', async () => {
    render(<UserList />);
    await waitFor(() => {
      expect(screen.getByText('普通用户')).toBeTruthy();
    });
    const row = document.querySelector('tr.ant-table-row') as HTMLElement;
    fireEvent.click(Array.from(row.querySelectorAll('button')).find((b) => b.textContent === '修改') as HTMLElement);
    await waitFor(() => {
      expect(document.querySelector('.ant-modal')).toBeTruthy();
    });
    const modal = document.querySelector('.ant-modal') as HTMLElement;
    fireEvent.change(modal.querySelector('input[placeholder="请输入显示名"]') as HTMLInputElement, {
      target: { value: '改后显示名' },
    });
    fireEvent.click(Array.from(modal.querySelectorAll('button')).find((b) => b.textContent === 'OK') as HTMLElement);
    await waitFor(() => {
      expect(mockPut).toHaveBeenCalled();
    });
    const [url, body] = mockPut.mock.calls[0] as [string, Record<string, unknown>];
    expect(url).toBe('/users/2');
    expect(body).not.toHaveProperty('enabled');
    expect(body.displayName).toBe('改后显示名');
    expect(body.password).toBeUndefined();
  });

  it('禁止登录：Popconfirm 确认后调用 PUT /users/{id} 传 { enabled: 0 }', async () => {
    render(<UserList />);
    await waitFor(() => {
      expect(screen.getByText('普通用户')).toBeTruthy();
    });
    const row = document.querySelector('tr.ant-table-row') as HTMLElement;
    fireEvent.click(Array.from(row.querySelectorAll('button')).find((b) => b.textContent === '禁止登录') as HTMLElement);
    await waitFor(() => {
      expect(document.querySelector('.ant-popconfirm')).toBeTruthy();
    });
    const pop = document.querySelector('.ant-popconfirm') as HTMLElement;
    fireEvent.click(Array.from(pop.querySelectorAll('button')).find((b) => b.textContent === 'OK') as HTMLElement);
    await waitFor(() => {
      expect(mockPut).toHaveBeenCalled();
    });
    expect(mockPut.mock.calls[0][0]).toBe('/users/2');
    expect((mockPut.mock.calls[0] as [string, Record<string, unknown>])[1]).toEqual({ enabled: 0 });
  });
});
