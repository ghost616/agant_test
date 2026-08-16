import { describe, it, expect, vi, beforeAll, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import type { Session } from '../../../types/session';

const mocks = vi.hoisted(() => ({
  listLogSessions: vi.fn(),
  navigate: vi.fn(),
}));

vi.mock('../../../services/session', () => ({
  listLogSessions: (...args: unknown[]) => mocks.listLogSessions(...args),
}));

vi.mock('react-router-dom', () => ({
  useNavigate: () => mocks.navigate,
}));

import SessionLogList from '../SessionLogList';

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

function makeSession(overrides: Partial<Session> = {}): Session {
  return {
    id: '100',
    agentId: 'agent-1',
    modelId: 'model-1',
    title: '主会话A',
    createTime: '2026-08-16 10:00:00',
    updateTime: '2026-08-16 10:00:00',
    ...overrides,
  };
}

describe('SessionLogList 会话日志页', () => {
  beforeEach(() => {
    mocks.listLogSessions.mockReset();
    mocks.navigate.mockReset();
    mocks.listLogSessions.mockResolvedValue([makeSession()]);
  });

  it('初始加载调用 listLogSessions 并渲染表格列', async () => {
    render(<SessionLogList />);
    await screen.findByText('主会话A');

    expect(mocks.listLogSessions).toHaveBeenCalledTimes(1);
    const headerTexts = Array.from(
      document.querySelectorAll('.ant-table-thead th'),
    ).map((el) => el?.textContent ?? '');
    for (const title of ['会话名', '是否评估', '创建时间', '操作']) {
      expect(headerTexts).toContain(title);
    }
  });

  it('isEvaluation=true 显示评估会话 Tag，false/缺省显示普通会话 Tag', async () => {
    mocks.listLogSessions.mockResolvedValue([
      makeSession({ id: '1', title: '评估会话A', isEvaluation: true }),
      makeSession({ id: '2', title: '普通会话B', isEvaluation: false }),
      makeSession({ id: '3', title: '普通会话C' }),
    ]);
    render(<SessionLogList />);
    await screen.findByText('评估会话A');

    const tags = Array.from(document.querySelectorAll('.ant-table-tbody .ant-tag')).map(
      (el) => el?.textContent?.trim() ?? '',
    );
    expect(tags).toContain('评估会话');
    expect(tags.filter((t) => t === '普通会话').length).toBe(2);
  });

  it('点击查看日志按钮跳转 /logs/{sessionId}', async () => {
    mocks.listLogSessions.mockResolvedValue([makeSession({ id: 'abc-123' })]);
    render(<SessionLogList />);

    const btn = await screen.findByRole('button', { name: '查看日志' });
    fireEvent.click(btn);

    expect(mocks.navigate).toHaveBeenCalledWith('/logs/abc-123');
  });

  it('会话名为空时显示占位符 -', async () => {
    mocks.listLogSessions.mockResolvedValue([makeSession({ title: undefined })]);
    render(<SessionLogList />);

    await waitFor(() => {
      expect(screen.getAllByText('-').length).toBeGreaterThan(0);
    });
  });

  it('加载失败提示错误信息', async () => {
    mocks.listLogSessions.mockRejectedValue(new Error('fail'));
    render(<SessionLogList />);

    await waitFor(() => {
      expect(screen.getByText('获取会话日志列表失败')).toBeTruthy();
    });
  });
});