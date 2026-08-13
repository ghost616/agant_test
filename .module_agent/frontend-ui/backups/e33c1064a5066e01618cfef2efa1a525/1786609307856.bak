import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import type { SessionMemoryDocument } from '../../../types/memory';

const mocks = vi.hoisted(() => ({
  getSessionMemory: vi.fn(),
}));
const mockNavigate = vi.hoisted(() => vi.fn());

let params: { sessionId?: string; type?: string } = { sessionId: '100', type: 'DAILY' };

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
  useParams: () => params,
}));

vi.mock('../../../services/memory', () => ({
  getSessionMemory: (...args: unknown[]) => mocks.getSessionMemory(...args),
}));

import MemoryDetail from '../MemoryDetail';

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

function makeDoc(overrides: Partial<SessionMemoryDocument> = {}): SessionMemoryDocument {
  return {
    sessionId: '100',
    aggregationType: 'DAILY',
    aggregationStartSeq: 1,
    aggregationEndSeq: 3,
    aggregationStartTime: 1720000000000,
    aggregationEndTime: 1720000000000,
    aggregationText: '摘要内容',
    ...overrides,
  };
}

function defaultResult(overrides: Partial<SessionMemoryDocument> = {}) {
  return { list: [makeDoc(overrides)], total: 1, page: 1, size: 20 };
}

function renderComponent() {
  return render(<MemoryDetail />);
}

function getHeaderTexts(): string[] {
  return Array.from(document.querySelectorAll('.ant-table-thead th')).map(
    (el) => el?.textContent ?? '',
  );
}

describe('MemoryDetail 按路由 type 查询', () => {
  beforeEach(() => {
    mocks.getSessionMemory.mockReset();
    mockNavigate.mockReset();
    params = { sessionId: '100', type: 'DAILY' };
  });

  it('DAILY 类型调用 getSessionMemory(sessionId, DAILY, page=1, size=20)', async () => {
    mocks.getSessionMemory.mockResolvedValue(defaultResult());
    renderComponent();
    await waitFor(() => {
      expect(mocks.getSessionMemory).toHaveBeenCalledWith('100', 'DAILY', 1, 20);
    });
  });

  it('GROUP 类型调用 getSessionMemory(sessionId, GROUP, page=1, size=20)', async () => {
    params = { sessionId: '100', type: 'GROUP' };
    mocks.getSessionMemory.mockResolvedValue(defaultResult({ aggregationType: 'GROUP' }));
    renderComponent();
    await waitFor(() => {
      expect(mocks.getSessionMemory).toHaveBeenCalledWith('100', 'GROUP', 1, 20);
    });
  });

  it('type 缺省时按 DAILY 处理', async () => {
    params = { sessionId: '100' };
    mocks.getSessionMemory.mockResolvedValue(defaultResult());
    renderComponent();
    await waitFor(() => {
      expect(mocks.getSessionMemory).toHaveBeenCalledWith('100', 'DAILY', 1, 20);
    });
  });
});

describe('MemoryDetail 列渲染', () => {
  beforeEach(() => {
    mocks.getSessionMemory.mockReset();
    mockNavigate.mockReset();
    params = { sessionId: '100', type: 'DAILY' };
  });

  it('DAILY 显示「聚合日期」列（aggregationStartTime 格式化为日期），不显示「起始-结束」', async () => {
    mocks.getSessionMemory.mockResolvedValue(defaultResult());
    renderComponent();
    await screen.findByText('摘要内容');

    const headerTexts = getHeaderTexts();
    expect(headerTexts).toContain('聚合日期');
    expect(headerTexts).not.toContain('起始-结束');

    const expectedDate = new Date(1720000000000).toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
    expect(screen.getAllByText(expectedDate).length).toBeGreaterThan(0);
  });

  it('aggregationStartTime 为空时聚合日期显示 -', async () => {
    mocks.getSessionMemory.mockResolvedValue(defaultResult({ aggregationStartTime: undefined }));
    renderComponent();
    await screen.findByText('摘要内容');

    const dateCells = Array.from(document.querySelectorAll('.ant-table-tbody td')).filter(
      (el) => el?.textContent?.trim() === '-',
    );
    expect(dateCells.length).toBeGreaterThan(0);
  });

  it('GROUP 显示「起始-结束」列（startSeq - endSeq），不显示「聚合日期」', async () => {
    params = { sessionId: '100', type: 'GROUP' };
    mocks.getSessionMemory.mockResolvedValue(defaultResult({ aggregationType: 'GROUP' }));
    renderComponent();
    await screen.findByText('摘要内容');

    const headerTexts = getHeaderTexts();
    expect(headerTexts).toContain('起始-结束');
    expect(headerTexts).not.toContain('聚合日期');
    expect(screen.getAllByText('1 - 3').length).toBeGreaterThan(0);
  });

  it('GROUP 行号缺省时显示 - - -', async () => {
    params = { sessionId: '100', type: 'GROUP' };
    mocks.getSessionMemory.mockResolvedValue(
      defaultResult({
        aggregationType: 'GROUP',
        aggregationStartSeq: undefined,
        aggregationEndSeq: undefined,
      }),
    );
    renderComponent();
    await screen.findByText('摘要内容');

    expect(screen.getAllByText('- - -').length).toBeGreaterThan(0);
  });

  it('聚合文本为空时显示 -', async () => {
    mocks.getSessionMemory.mockResolvedValue(defaultResult({ aggregationText: undefined }));
    renderComponent();
    await waitFor(() => {
      expect(document.querySelector('tr.ant-table-row')).toBeTruthy();
    });

    const dashCells = Array.from(document.querySelectorAll('.ant-table-tbody td')).filter(
      (el) => el?.textContent?.trim() === '-',
    );
    expect(dashCells.length).toBeGreaterThan(0);
  });

  it('标题随 type 变化：DAILY=按日聚合记忆、GROUP=按分类聚合记忆', async () => {
    mocks.getSessionMemory.mockResolvedValue(defaultResult());
    renderComponent();
    await screen.findByText('按日聚合记忆');

    params = { sessionId: '100', type: 'GROUP' };
    mocks.getSessionMemory.mockResolvedValue(defaultResult({ aggregationType: 'GROUP' }));
    const { unmount } = renderComponent();
    await waitFor(() => {
      expect(screen.getAllByText('按分类聚合记忆').length).toBeGreaterThan(0);
    });
    unmount();
  });
});

describe('MemoryDetail 分页', () => {
  beforeEach(() => {
    mocks.getSessionMemory.mockReset();
    mockNavigate.mockReset();
    params = { sessionId: '100', type: 'DAILY' };
  });

  it('页码变化后重新请求携带新 page', async () => {
    mocks.getSessionMemory.mockResolvedValue({
      list: [makeDoc()],
      total: 100,
      page: 1,
      size: 20,
    });
    renderComponent();
    await screen.findByText('摘要内容');

    const page2 = document.querySelector('.ant-pagination-item-2') as HTMLElement;
    expect(page2).toBeTruthy();
    fireEvent.click(page2);

    await waitFor(() => {
      expect(mocks.getSessionMemory).toHaveBeenCalledTimes(2);
    });
    const lastCall = mocks.getSessionMemory.mock.calls[1] as [
      string,
      string,
      number,
      number,
    ];
    expect(lastCall[2]).toBe(2);
    expect(lastCall[3]).toBe(20);
  });

  it('pageSizeOptions 为 10/20/50 且启用每页条数切换', async () => {
    mocks.getSessionMemory.mockResolvedValue({
      list: [makeDoc()],
      total: 100,
      page: 1,
      size: 20,
    });
    renderComponent();
    await screen.findByText('摘要内容');

    const sizeChanger = document.querySelector(
      '.ant-pagination-options .ant-select .ant-select-selector',
    ) as HTMLElement;
    expect(sizeChanger).toBeTruthy();
    fireEvent.mouseDown(sizeChanger);
    await waitFor(() => {
      const options = Array.from(
        document.querySelectorAll('.ant-select-item-option-content'),
      ).map((el) => el?.textContent?.trim() ?? '');
      expect(options.some((t) => t.includes('10'))).toBe(true);
      expect(options.some((t) => t.includes('20'))).toBe(true);
      expect(options.some((t) => t.includes('50'))).toBe(true);
    });
  });

  it('showTotal 展示共 N 条', async () => {
    mocks.getSessionMemory.mockResolvedValue({
      list: [makeDoc()],
      total: 100,
      page: 1,
      size: 20,
    });
    renderComponent();
    await screen.findByText('摘要内容');

    expect(screen.getAllByText('共 100 条').length).toBeGreaterThan(0);
  });

  it('切到第 2 页后切换每页条数，重置到第 1 页并携带新 size', async () => {
    mocks.getSessionMemory.mockResolvedValue({
      list: [makeDoc()],
      total: 100,
      page: 1,
      size: 20,
    });
    renderComponent();
    await screen.findByText('摘要内容');

    const page2 = document.querySelector('.ant-pagination-item-2') as HTMLElement;
    fireEvent.click(page2);
    await waitFor(() => {
      expect(mocks.getSessionMemory).toHaveBeenCalledTimes(2);
    });
    const page2Call = mocks.getSessionMemory.mock.calls[1] as [
      string,
      string,
      number,
      number,
    ];
    expect(page2Call[2]).toBe(2);
    expect(page2Call[3]).toBe(20);

    const sizeChanger = document.querySelector(
      '.ant-pagination-options .ant-select .ant-select-selector',
    ) as HTMLElement;
    fireEvent.mouseDown(sizeChanger);
    const option50 = await screen.findByText(/50/, {
      selector: '.ant-select-item-option-content',
    });
    fireEvent.click(option50);

    await waitFor(() => {
      expect(mocks.getSessionMemory).toHaveBeenCalledTimes(3);
    });
    const lastCall = mocks.getSessionMemory.mock.calls[2] as [
      string,
      string,
      number,
      number,
    ];
    expect(lastCall[2]).toBe(1);
    expect(lastCall[3]).toBe(50);
  });
});

describe('MemoryDetail 返回按钮与源码配置', () => {
  beforeEach(() => {
    mocks.getSessionMemory.mockReset();
    mockNavigate.mockReset();
    params = { sessionId: '100', type: 'DAILY' };
  });

  it('返回按钮跳转 /memory', async () => {
    mocks.getSessionMemory.mockResolvedValue(defaultResult());
    renderComponent();
    await screen.findByText('摘要内容');

    fireEvent.click(screen.getByText('返回'));
    expect(mockNavigate).toHaveBeenCalledWith('/memory');
  });

  it('聚合文本列配置 ellipsis，分页配置 pageSizeOptions/showTotal', () => {
    const source = readFileSync(resolve(__dirname, '../MemoryDetail.tsx'), 'utf-8');
    expect(source).toContain('ellipsis: true');
    expect(source).toContain('pageSizeOptions: PAGE_SIZE_OPTIONS');
    expect(source).toContain('showTotal');
    expect(source).toContain('PAGE_SIZE_OPTIONS = [10, 20, 50]');
  });
});

describe('MemoryDetail 表格滚动 (useTableScrollY)', () => {
  it('表格 scroll 使用 useTableScrollY 实现固定表头动态高度', () => {
    const source = readFileSync(resolve(__dirname, '../MemoryDetail.tsx'), 'utf-8');
    expect(source).toContain("import useTableScrollY from '../../hooks/useTableScrollY'");
    expect(source).toContain('scroll={{ x: 820, y: useTableScrollY(272) }}');
  });
});
