import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import type { SessionMessage } from '../../../types/session';

const mocks = vi.hoisted(() => ({
  getSessionMessagesRange: vi.fn(),
}));
const mockNavigate = vi.hoisted(() => vi.fn());

let params: { sessionId?: string; type?: string; seqRange?: string } = {
  sessionId: '100',
  type: 'DAILY',
  seqRange: '1-3',
};
let locationState: Record<string, unknown> | null = {
  startSeq: 1,
  endSeq: 3,
  aggregationText: '聚合文本内容',
};

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
  useParams: () => params,
  useLocation: () => ({ state: locationState }),
}));

vi.mock('../../../services/session', () => ({
  getSessionMessagesRange: (...args: unknown[]) => mocks.getSessionMessagesRange(...args),
}));

import MemoryDocumentDetail from '../MemoryDocumentDetail';

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

function makeMessage(overrides: Partial<SessionMessage> = {}): SessionMessage {
  return {
    id: 'm1',
    sessionId: '100',
    role: 'user',
    content: '你好',
    sequenceNum: 1,
    createTime: '2026-08-01 09:00:00',
    ...overrides,
  };
}

function renderComponent() {
  return render(<MemoryDocumentDetail />);
}

describe('MemoryDocumentDetail 消息加载', () => {
  beforeEach(() => {
    mocks.getSessionMessagesRange.mockReset();
    mockNavigate.mockReset();
    params = { sessionId: '100', type: 'DAILY', seqRange: '1-3' };
    locationState = { startSeq: 1, endSeq: 3, aggregationText: '聚合文本内容' };
  });

  it('根据路由 state 中的 startSeq/endSeq 调用 getSessionMessagesRange', async () => {
    mocks.getSessionMessagesRange.mockResolvedValue([makeMessage()]);
    renderComponent();
    await waitFor(() => {
      expect(mocks.getSessionMessagesRange).toHaveBeenCalledWith('100', 1, 3);
    });
  });

  it('state 缺失时从 seqRange 路由参数解析 startSeq/endSeq', async () => {
    locationState = null;
    mocks.getSessionMessagesRange.mockResolvedValue([makeMessage()]);
    renderComponent();
    await waitFor(() => {
      expect(mocks.getSessionMessagesRange).toHaveBeenCalledWith('100', 1, 3);
    });
  });

  it('state 缺失时聚合文本显示为空', async () => {
    locationState = null;
    mocks.getSessionMessagesRange.mockResolvedValue([makeMessage()]);
    renderComponent();
    await waitFor(() => {
      expect(mocks.getSessionMessagesRange).toHaveBeenCalled();
    });
    const textarea = document.querySelector(
      'textarea.ant-input',
    ) as HTMLTextAreaElement;
    expect(textarea).toBeTruthy();
    expect(textarea.readOnly).toBe(true);
    expect(textarea.value).toBe('');
  });

  it('API 失败时提示错误并清空消息列表', async () => {
    mocks.getSessionMessagesRange.mockRejectedValue(new Error('Network Error'));
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText('获取消息列表失败')).toBeTruthy();
    });
  });
});

describe('MemoryDocumentDetail 渲染', () => {
  beforeEach(() => {
    mocks.getSessionMessagesRange.mockReset();
    mockNavigate.mockReset();
    params = { sessionId: '100', type: 'DAILY', seqRange: '1-3' };
    locationState = { startSeq: 1, endSeq: 3, aggregationText: '聚合文本内容' };
  });

  it('左侧文本框只读展示聚合文本，右侧展示消息角色标签', async () => {
    mocks.getSessionMessagesRange.mockResolvedValue([
      makeMessage({ id: 'm1', role: 'user', content: '你好', sequenceNum: 1 }),
      makeMessage({
        id: 'm2',
        role: 'assistant',
        content: '收到',
        sequenceNum: 2,
        createTime: '2026-08-01 09:00:01',
      }),
    ]);
    renderComponent();

    const textarea = (await screen.findByDisplayValue('聚合文本内容')) as HTMLTextAreaElement;
    expect(textarea.readOnly).toBe(true);

    await waitFor(() => {
      expect(screen.getByText('你')).toBeTruthy();
      expect(screen.getByText('助手')).toBeTruthy();
      expect(screen.getByText('你好')).toBeTruthy();
      expect(screen.getByText('收到')).toBeTruthy();
    });
  });

  it('消息按 sequenceNum 升序排列渲染（乱序输入仍按序输出）', async () => {
    mocks.getSessionMessagesRange.mockResolvedValue([
      makeMessage({ id: 'm3', role: 'assistant', content: '第三条', sequenceNum: 3 }),
      makeMessage({ id: 'm1', role: 'user', content: '第一条', sequenceNum: 1 }),
      makeMessage({ id: 'm2', role: 'assistant', content: '第二条', sequenceNum: 2 }),
    ]);
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('第一条')).toBeTruthy();
    });

    const bubbles = Array.from(document.querySelectorAll('.agent-chat-markdown'));
    const texts = bubbles.map((b) => b.textContent ?? '');
    expect(texts).toEqual(['第一条', '第二条', '第三条']);
  });

  it('返回按钮跳转 /memory/100/DAILY', async () => {
    mocks.getSessionMessagesRange.mockResolvedValue([makeMessage()]);
    renderComponent();
    await waitFor(() => {
      expect(mocks.getSessionMessagesRange).toHaveBeenCalled();
    });

    fireEvent.click(screen.getByText('返回'));
    expect(mockNavigate).toHaveBeenCalledWith('/memory/100/DAILY');
  });

  it('GROUP 类型返回按钮跳转 /memory/100/GROUP', async () => {
    params = { sessionId: '100', type: 'GROUP', seqRange: '1-3' };
    mocks.getSessionMessagesRange.mockResolvedValue([makeMessage()]);
    renderComponent();
    await waitFor(() => {
      expect(mocks.getSessionMessagesRange).toHaveBeenCalled();
    });

    fireEvent.click(screen.getByText('返回'));
    expect(mockNavigate).toHaveBeenCalledWith('/memory/100/GROUP');
  });
});

describe('MemoryDocumentDetail 源码配置', () => {
  it('调用 getSessionMessagesRange 接口并支持只读文本框', () => {
    const source = readFileSync(resolve(__dirname, '../MemoryDocumentDetail.tsx'), 'utf-8');
    expect(source).toContain('getSessionMessagesRange');
    expect(source).toContain('readOnly');
  });

  it('消息以对话气泡样式渲染（ROLE_CONFIG/BUBBLE_STYLES + Markdown）', () => {
    const source = readFileSync(resolve(__dirname, '../MemoryDocumentDetail.tsx'), 'utf-8');
    expect(source).toContain('ROLE_CONFIG');
    expect(source).toContain('BUBBLE_STYLES');
    expect(source).toContain('ReactMarkdown remarkPlugins={[remarkGfm]}');
    expect(source).toContain('agent-chat-markdown');
  });

  it('消息按 sequenceNum 升序排列', () => {
    const source = readFileSync(resolve(__dirname, '../MemoryDocumentDetail.tsx'), 'utf-8');
    expect(source).toContain('.sort((a, b) => a.sequenceNum - b.sequenceNum)');
  });

  it('左右两栏等高容器布局，各自内容区独立滚动', () => {
    const source = readFileSync(resolve(__dirname, '../MemoryDocumentDetail.tsx'), 'utf-8');
    expect(source).toContain("alignItems: 'stretch'");
    expect(source).toContain("overflowY: 'auto'");
    expect(source).toContain('minHeight: 0');
  });

  it('messages/range URL 封装在 services/session.ts', () => {
    const serviceSource = readFileSync(resolve(__dirname, '../../../services/session.ts'), 'utf-8');
    expect(serviceSource).toContain('`/sessions/${sessionId}/messages/range`');
  });
});
