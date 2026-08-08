import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import type { ToolConfig } from '../../../types/tool';

beforeAll(() => {
  window.matchMedia = window.matchMedia || ((query: string) => ({
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

vi.mock('../../../services/tool', () => ({
  listTools: vi.fn().mockResolvedValue([
    {
      id: 'tool-1',
      name: 'test_tool',
      toolType: 'JAVA',
      description: '测试工具',
      parameterSchema: '',
      returnSchema: '',
      implPath: '/impl/test',
      status: 'ENABLED',
      createTime: '2026-07-11T03:00:00Z',
      updateTime: '2026-07-11T03:00:00Z',
    } as ToolConfig,
  ]),
  createTool: vi.fn().mockResolvedValue({ id: 'tool-new' }),
  updateTool: vi.fn().mockResolvedValue({ id: 'tool-1' }),
  deleteTool: vi.fn().mockResolvedValue(undefined),
  updateToolStatus: vi.fn().mockResolvedValue(undefined),
}));

import ToolList from '../ToolList';

describe('ToolList JsonEditor 懒加载渲染', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('组件应正常渲染工具列表，不因懒加载崩溃', async () => {
    render(<ToolList />);
    await waitFor(() => {
      expect(screen.getByText('测试工具')).toBeTruthy();
    });
    expect(screen.getByText('test_tool')).toBeTruthy();
  });

  it('打开新增 Modal 且选择非 CUSTOM/MCP_HTTP 类型时 JsonEditor 懒加载不崩溃', async () => {
    render(<ToolList />);
    await waitFor(() => {
      expect(screen.getByText('测试工具')).toBeTruthy();
    });

    fireEvent.click(screen.getByText('新增工具'));
    await waitFor(() => {
      expect(document.querySelector('.ant-modal-content')).toBeTruthy();
    });

    const typeSelect = document.querySelector('.ant-modal-content .ant-select');
    expect(typeSelect).toBeTruthy();

    expect(screen.getByText('参数 Schema')).toBeTruthy();
    expect(screen.getByText('返回 Schema')).toBeTruthy();
    expect(document.querySelectorAll('.ant-modal-content').length).toBe(1);
  });
});
