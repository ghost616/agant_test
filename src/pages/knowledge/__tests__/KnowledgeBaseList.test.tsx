import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { KnowledgeBase } from '../../../types/knowledge';

const mockNavigate = vi.fn();

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

const mocks = {
  listKnowledgeBases: vi.fn(),
  createKnowledgeBase: vi.fn(),
  updateKnowledgeBase: vi.fn(),
  deleteKnowledgeBase: vi.fn(),
  updateKnowledgeBaseStatus: vi.fn(),
  rebuildKnowledgeBaseES: vi.fn(),
  listModels: vi.fn(),
};

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({}),
  };
});

vi.mock('../../../services/knowledge', () => ({
  listKnowledgeBases: (...args: unknown[]) => mocks.listKnowledgeBases(...args),
  createKnowledgeBase: (...args: unknown[]) => mocks.createKnowledgeBase(...args),
  updateKnowledgeBase: (...args: unknown[]) => mocks.updateKnowledgeBase(...args),
  deleteKnowledgeBase: (...args: unknown[]) => mocks.deleteKnowledgeBase(...args),
  updateKnowledgeBaseStatus: (...args: unknown[]) => mocks.updateKnowledgeBaseStatus(...args),
  rebuildKnowledgeBaseES: (...args: unknown[]) => mocks.rebuildKnowledgeBaseES(...args),
}));

vi.mock('../../../services/model', () => ({
  listModels: (...args: unknown[]) => mocks.listModels(...args),
}));

import KnowledgeBaseList from '../KnowledgeBaseList';

function makeKb(overrides: Partial<KnowledgeBase> = {}): KnowledgeBase {
  return {
    id: 'kb-1',
    name: '知识库A',
    description: '描述',
    status: 'ENABLED',
    createTime: '2026-08-01T00:00:00',
    updateTime: '2026-08-01T00:00:00',
    ...overrides,
  };
}

function renderComponent() {
  return render(
    <MemoryRouter>
      <KnowledgeBaseList />
    </MemoryRouter>,
  );
}

describe('KnowledgeBaseList ES 数据重构按钮 (功能点5)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mocks.listKnowledgeBases.mockReset();
    mocks.rebuildKnowledgeBaseES.mockReset();
    mocks.listModels.mockReset();
    mocks.listKnowledgeBases.mockResolvedValue([makeKb()]);
    mocks.listModels.mockResolvedValue([]);
  });

  it('点击 ES数据重构 调用 rebuildKnowledgeBaseES(kbId) 并提示「ES 数据重构已触发」', async () => {
    mocks.rebuildKnowledgeBaseES.mockResolvedValue(undefined);
    renderComponent();
    const btn = await screen.findByRole('button', { name: 'ES数据重构' });
    fireEvent.click(btn);
    await waitFor(() => {
      expect(mocks.rebuildKnowledgeBaseES).toHaveBeenCalledWith('kb-1');
    });
    await waitFor(() => {
      expect(document.body.textContent).toContain('ES 数据重构已触发');
    });
  });

  it('重构失败应提示「ES 数据重构失败」', async () => {
    mocks.rebuildKnowledgeBaseES.mockRejectedValue(new Error('失败'));
    renderComponent();
    const btn = await screen.findByRole('button', { name: 'ES数据重构' });
    fireEvent.click(btn);
    await waitFor(() => {
      expect(document.body.textContent).toContain('ES 数据重构失败');
    });
  });
});

describe('KnowledgeBaseList rebuilding=true 时管理文件禁用 (功能点6)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mocks.listKnowledgeBases.mockReset();
    mocks.rebuildKnowledgeBaseES.mockReset();
    mocks.listModels.mockReset();
    mocks.listKnowledgeBases.mockResolvedValue([makeKb({ rebuilding: true })]);
    mocks.listModels.mockResolvedValue([]);
  });

  it('管理文件按钮应 disabled', async () => {
    renderComponent();
    const btn = await screen.findByRole('button', { name: '管理文件' });
    expect((btn as HTMLButtonElement).disabled).toBe(true);
  });

  it('rebuilding=false 时管理文件按钮可点击', async () => {
    mocks.listKnowledgeBases.mockResolvedValue([makeKb({ rebuilding: false })]);
    renderComponent();
    const btn = await screen.findByRole('button', { name: '管理文件' });
    expect((btn as HTMLButtonElement).disabled).toBe(false);
  });
});

describe('KnowledgeBaseList 编辑弹窗 (功能点7)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mocks.listKnowledgeBases.mockReset();
    mocks.rebuildKnowledgeBaseES.mockReset();
    mocks.listModels.mockReset();
    mocks.listKnowledgeBases.mockResolvedValue([makeKb()]);
  });

  it('挂载时调用 listModels 且参数 modelType=EMBEDDINGS', async () => {
    mocks.listModels.mockResolvedValue([
      { id: 'vm-1', name: '向量模型A', platformType: 'openai', modelName: 'embed-1' },
    ]);
    renderComponent();
    await waitFor(() => {
      expect(mocks.listModels).toHaveBeenCalledWith({ modelType: 'EMBEDDINGS' });
    });
  });

  it('新增弹窗应包含向量模型下拉与 ES 索引输入框', async () => {
    mocks.listModels.mockResolvedValue([]);
    renderComponent();
    fireEvent.click(screen.getByText('新增知识库'));
    await waitFor(() => {
      expect(screen.getByText('新增知识库', { selector: '.ant-modal-title' })).toBeTruthy();
    });
    const modal = document.querySelector('.ant-modal');
    expect(
      modal?.querySelector('.ant-select-selection-placeholder')?.textContent,
    ).toContain('请选择向量模型');
    expect(screen.getByLabelText('ES 索引')).toBeTruthy();
  });

  it('编辑回填 vectorModelId 与 esIndex', async () => {
    mocks.listModels.mockResolvedValue([
      { id: 'vm-1', name: '向量模型A', platformType: 'openai', modelName: 'embed-1' },
    ]);
    mocks.listKnowledgeBases.mockResolvedValue([
      makeKb({ id: 'kb-1', vectorModelId: 'vm-1', esIndex: 'es-index-1' }),
    ]);
    renderComponent();
    await screen.findByText('知识库A');
    fireEvent.click(screen.getByRole('button', { name: '编辑' }));
    await waitFor(() => {
      const esInput = screen.getByLabelText('ES 索引') as HTMLInputElement;
      expect(esInput.value).toBe('es-index-1');
    });
    await waitFor(() => {
      expect(document.querySelector('.ant-select-selection-item')?.textContent).toContain('向量模型A');
    });
  });
});
