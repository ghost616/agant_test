import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { KnowledgeFile } from '../../../types/knowledge';

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
  getKnowledgeFile: vi.fn(),
  getKnowledgeFileContent: vi.fn(),
  updateKnowledgeFileContent: vi.fn(),
};

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ kbId: 'kb-1', fileId: 'file-1' }),
  };
});

vi.mock('../../../services/knowledge', () => ({
  getKnowledgeFile: (...args: unknown[]) => mocks.getKnowledgeFile(...args),
  getKnowledgeFileContent: (...args: unknown[]) =>
    mocks.getKnowledgeFileContent(...args),
  updateKnowledgeFileContent: (...args: unknown[]) =>
    mocks.updateKnowledgeFileContent(...args),
}));

import KnowledgeFileEdit from '../KnowledgeFileEdit';

function makeFile(overrides: Partial<KnowledgeFile> = {}): KnowledgeFile {
  return {
    id: 'file-1',
    fileName: '知识文档.md',
    fileDescription: '测试文件',
    knowledgeBaseId: 'kb-1',
    status: 'ENABLED',
    createTime: '2026-08-01T00:00:00',
    updateTime: '2026-08-01T00:00:00',
    ...overrides,
  };
}

function renderComponent() {
  return render(
    <MemoryRouter>
      <KnowledgeFileEdit />
    </MemoryRouter>,
  );
}

describe('KnowledgeFileEdit publishStatus=PUBLISHING 禁用编辑 (功能点8)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mocks.getKnowledgeFile.mockReset();
    mocks.getKnowledgeFileContent.mockReset();
    mocks.updateKnowledgeFileContent.mockReset();
  });

  it('应显示「发布中，暂不可编辑」Tag', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(
      makeFile({ publishStatus: 'PUBLISHING' }),
    );
    mocks.getKnowledgeFileContent.mockResolvedValue('# 内容');
    renderComponent();
    expect(await screen.findByText('发布中，暂不可编辑')).toBeTruthy();
  });

  it('TextArea 应被禁用', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(
      makeFile({ publishStatus: 'PUBLISHING' }),
    );
    mocks.getKnowledgeFileContent.mockResolvedValue('# 内容');
    renderComponent();
    await screen.findByText('发布中，暂不可编辑');
    const textarea = screen.getByPlaceholderText('请输入 Markdown 内容') as HTMLTextAreaElement;
    expect(textarea.disabled).toBe(true);
  });

  it('保存按钮应被禁用', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(
      makeFile({ publishStatus: 'PUBLISHING' }),
    );
    mocks.getKnowledgeFileContent.mockResolvedValue('# 内容');
    renderComponent();
    await screen.findByText('发布中，暂不可编辑');
    const saveBtn = screen.getByRole('button', { name: /保\s*存/ }) as HTMLButtonElement;
    expect(saveBtn.disabled).toBe(true);
  });

  it('publishStatus 非 PUBLISHING 时 TextArea 与保存按钮可用', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(
      makeFile({ publishStatus: 'UNPUBLISHED' }),
    );
    mocks.getKnowledgeFileContent.mockResolvedValue('# 内容');
    renderComponent();
    await screen.findByText('知识文档.md');
    const textarea = screen.getByPlaceholderText('请输入 Markdown 内容') as HTMLTextAreaElement;
    const saveBtn = screen.getByRole('button', { name: /保\s*存/ }) as HTMLButtonElement;
    expect(textarea.disabled).toBe(false);
    expect(saveBtn.disabled).toBe(false);
    expect(screen.queryByText('发布中，暂不可编辑')).toBeNull();
  });
});
