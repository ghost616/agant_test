import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { ModelConfig } from '../../../types/model';

const mockNavigate = vi.hoisted(() => vi.fn());
const mockEmbed = vi.hoisted(() => vi.fn());
const mockMessageError = vi.hoisted(() => vi.fn());

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ id: 'test-id' }),
  };
});

vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return {
    ...actual,
    message: {
      error: mockMessageError,
      warning: vi.fn(),
    },
  };
});

vi.mock('../../../services/model', () => ({
  getModel: vi.fn().mockResolvedValue({
    id: 'test-id',
    name: 'Embed Model',
    platformType: 'OPENAI',
    modelType: 'EMBEDDINGS',
    apiKey: '',
    baseUrl: '',
    modelName: 'text-embedding-3',
    temperature: 0.7,
    maxTokens: 2048,
    status: 'ENABLED',
    description: '',
    createTime: '',
    updateTime: '',
  } satisfies ModelConfig),
  embed: mockEmbed,
  chatStream: vi.fn(),
}));

import ModelTest from '../ModelTest';

function renderComponent() {
  return render(
    <MemoryRouter>
      <ModelTest />
    </MemoryRouter>,
  );
}

describe('ModelTest EMBEDDINGS 分支', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mockEmbed.mockReset();
    mockMessageError.mockClear();
  });

  it('modelType 为 EMBEDDINGS 时应渲染嵌入测试界面（而非对话界面）', async () => {
    renderComponent();
    expect(await screen.findByText('输入文本获取向量表示')).toBeTruthy();
    expect(screen.getByPlaceholderText('输入文本，最多 1000 字符')).toBeTruthy();
    expect(screen.queryByText('思考模式')).toBeNull();
    expect(screen.queryByPlaceholderText('输入消息，Enter 发送，Shift+Enter 换行')).toBeNull();
  });

  it('页面头部应显示模型名称、平台标签与 modelType 标签', async () => {
    renderComponent();
    expect(await screen.findByText('Embed Model')).toBeTruthy();
    expect(screen.getByText('OPENAI')).toBeTruthy();
    expect(screen.getByText('EMBEDDINGS')).toBeTruthy();
  });

  it('输入超过 1000 字符时应报错并截断为 1000 字符', async () => {
    renderComponent();
    const textarea = await screen.findByPlaceholderText('输入文本，最多 1000 字符');
    const longText = 'a'.repeat(1005);
    fireEvent.change(textarea, { target: { value: longText } });
    expect(mockMessageError).toHaveBeenCalledWith('输入内容不能超过 1000 字符');
    expect((textarea as HTMLTextAreaElement).value).toHaveLength(1000);
  });

  it('输入等于 1000 字符时不应报错', async () => {
    renderComponent();
    const textarea = await screen.findByPlaceholderText('输入文本，最多 1000 字符');
    const boundaryText = 'b'.repeat(1000);
    fireEvent.change(textarea, { target: { value: boundaryText } });
    expect(mockMessageError).not.toHaveBeenCalled();
    expect((textarea as HTMLTextAreaElement).value).toHaveLength(1000);
  });

  it('点击发送应调用 embed API 并传入模型名', async () => {
    mockEmbed.mockResolvedValueOnce({
      embeddings: [{ index: 0, embedding: [0.1, 0.2] }],
      usage: { promptTokens: 1, completionTokens: 0, totalTokens: 1 },
    });
    renderComponent();
    const textarea = await screen.findByPlaceholderText('输入文本，最多 1000 字符');
    fireEvent.change(textarea, { target: { value: '你好世界' } });
    fireEvent.click(screen.getByRole('button', { name: '发 送' }));
    await waitFor(() => {
      expect(mockEmbed).toHaveBeenCalledWith('test-id', {
        input: '你好世界',
        model: 'text-embedding-3',
      });
    });
  });

  it('embed 请求期间发送按钮应处于加载禁用状态', async () => {
    let resolveFn!: (v: unknown) => void;
    mockEmbed.mockImplementationOnce(
      () => new Promise((resolve) => { resolveFn = resolve; }),
    );
    renderComponent();
    const textarea = await screen.findByPlaceholderText('输入文本，最多 1000 字符');
    fireEvent.change(textarea, { target: { value: '加载测试' } });
    const sendButton = screen.getByRole('button', { name: '发 送' });
    fireEvent.click(sendButton);
    expect(sendButton.className).toContain('ant-btn-loading');
    resolveFn({ embeddings: [{ index: 0, embedding: [0.1] }] });
    await waitFor(() => expect(sendButton.className).not.toContain('ant-btn-loading'));
  });

  it('空输入时发送按钮应禁用', async () => {
    renderComponent();
    const sendButton = await screen.findByRole('button', { name: '发 送' });
    expect(sendButton.hasAttribute('disabled')).toBe(true);
  });

  it('返回结果显示应展示向量，维度不超过 100 时无省略号', async () => {
    mockEmbed.mockResolvedValueOnce({
      embeddings: [{ index: 0, embedding: [0.123456, 0.654321] }],
    });
    renderComponent();
    const textarea = await screen.findByPlaceholderText('输入文本，最多 1000 字符');
    fireEvent.change(textarea, { target: { value: '短向量' } });
    fireEvent.click(screen.getByRole('button', { name: '发 送' }));
    expect(await screen.findByText(/\[0\.123456, 0\.654321\]/)).toBeTruthy();
  });

  it('维度超过 100 时结果应展示前 100 维并追加省略号', async () => {
    const embedding = Array.from({ length: 150 }, (_, i) => i / 100);
    mockEmbed.mockResolvedValueOnce({ embeddings: [{ index: 0, embedding }] });
    renderComponent();
    const textarea = await screen.findByPlaceholderText('输入文本，最多 1000 字符');
    fireEvent.change(textarea, { target: { value: '长向量' } });
    fireEvent.click(screen.getByRole('button', { name: '发 送' }));
    const textEl = await screen.findByText(/^\[.*\.\.\.\]$/);
    expect(textEl.textContent).toBeTruthy();
    const shown = textEl.textContent as string;
    expect(shown.startsWith('[')).toBe(true);
    expect(shown.endsWith('...]')).toBe(true);
    expect(shown.split(', ').length - 1).toBeLessThanOrEqual(101);
  });

  it('embed 失败时应 message.error 提示', async () => {
    mockEmbed.mockRejectedValueOnce(new Error('嵌入请求失败'));
    renderComponent();
    const textarea = await screen.findByPlaceholderText('输入文本，最多 1000 字符');
    fireEvent.change(textarea, { target: { value: '触发错误' } });
    fireEvent.click(screen.getByRole('button', { name: '发 送' }));
    await waitFor(() => {
      expect(mockMessageError).toHaveBeenCalledWith('嵌入请求失败');
    });
  });

  it('点击清空按钮应清空输入与结果', async () => {
    mockEmbed.mockResolvedValueOnce({ embeddings: [{ index: 0, embedding: [0.1] }] });
    renderComponent();
    const textarea = await screen.findByPlaceholderText('输入文本，最多 1000 字符');
    fireEvent.change(textarea, { target: { value: '待清空内容' } });
    fireEvent.click(screen.getByRole('button', { name: '发 送' }));
    await screen.findByText(/\[0\.100000\]/);
    fireEvent.click(screen.getByRole('button', { name: '清 空' }));
    expect((textarea as HTMLTextAreaElement).value).toBe('');
    expect(screen.queryByText(/\[0\.100000\]/)).toBeNull();
    expect(screen.getByText('输入文本获取向量表示')).toBeTruthy();
  });
});
