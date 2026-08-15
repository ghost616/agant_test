import { describe, it, expect, vi, beforeAll, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import type { User } from '../../../types/user';

const mocks = vi.hoisted(() => ({
  login: vi.fn(),
  navigate: vi.fn(),
}));

vi.mock('../../../services/auth', () => ({
  login: (...args: unknown[]) => mocks.login(...args),
}));

vi.mock('react-router-dom', () => ({
  useNavigate: () => mocks.navigate,
}));

import Login from '../Login';

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
});

function makeUser(overrides: Partial<User> = {}): User {
  return {
    id: '1',
    loginName: 'admin',
    displayName: '管理员',
    userType: 2,
    enabled: 1,
    ...overrides,
  };
}

async function submitLoginForm(): Promise<void> {
  fireEvent.change(screen.getByPlaceholderText('请输入登录名'), {
    target: { value: 'admin' },
  });
  fireEvent.change(screen.getByPlaceholderText('请输入密码'), {
    target: { value: '123456' },
  });
  fireEvent.click(screen.getByRole('button', { name: /登\s*录/ }));
}

describe('Login 登录页角色落地页跳转', () => {
  beforeEach(() => {
    mocks.login.mockReset();
    mocks.navigate.mockReset();
  });

  it('管理员（userType=2）登录成功后跳转 /users', async () => {
    mocks.login.mockResolvedValue(makeUser({ userType: 2 }));
    render(<Login />);

    await submitLoginForm();

    await waitFor(() => {
      expect(mocks.navigate).toHaveBeenCalledWith('/users');
    });
  });

  it('普通用户（userType=1）登录成功后跳转 /models', async () => {
    mocks.login.mockResolvedValue(makeUser({ userType: 1 }));
    render(<Login />);

    await submitLoginForm();

    await waitFor(() => {
      expect(mocks.navigate).toHaveBeenCalledWith('/models');
    });
  });

  it('登录失败时不跳转并提示错误信息', async () => {
    mocks.login.mockRejectedValue(new Error('账号或密码错误'));
    render(<Login />);

    await submitLoginForm();

    await waitFor(() => {
      expect(mocks.navigate).not.toHaveBeenCalled();
    });
  });
});
