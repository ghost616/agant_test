import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';
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

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ kbId: 'kb-1' }),
  };
});

vi.mock('../../../services/knowledge', () => ({
  listKnowledgeFiles: vi.fn().mockResolvedValue([
    {
      id: 'file-1',
      fileName: '知识文档.md',
      fileDescription: '测试文件',
      knowledgeBaseId: 'kb-1',
      status: 'ENABLED',
      createTime: '2026-08-01T00:00:00',
      updateTime: '2026-08-01T00:00:00',
    } satisfies KnowledgeFile,
  ]),
  createKnowledgeFile: vi.fn().mockResolvedValue({ id: 'file-1' }),
  updateKnowledgeFile: vi.fn().mockResolvedValue({ id: 'file-1' }),
  deleteKnowledgeFile: vi.fn().mockResolvedValue(undefined),
  updateKnowledgeFileStatus: vi.fn().mockResolvedValue(undefined),
}));

import KnowledgeFileList from '../KnowledgeFileList';

function renderComponent() {
  return render(
    <MemoryRouter>
      <KnowledgeFileList />
    </MemoryRouter>,
  );
}

describe('KnowledgeFileList 新建/编辑弹窗字段 (功能点1)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it('点击新增文件应打开弹窗且包含文件名与描述字段', async () => {
    renderComponent();
    fireEvent.click(screen.getByText('新增文件'));
    await waitFor(() => {
      expect(screen.getByText('新增文件', { selector: '.ant-modal-title' })).toBeTruthy();
    });
    expect(screen.getByLabelText('文件名')).toBeTruthy();
    expect(screen.getByLabelText('描述')).toBeTruthy();
  });

  it('弹窗中不应包含 fileContent 字段（无「文件内容」标签/无 fileContent name）', async () => {
    renderComponent();
    fireEvent.click(screen.getByText('新增文件'));
    await waitFor(() => {
      expect(screen.getByLabelText('文件名')).toBeTruthy();
    });
    expect(screen.queryByLabelText('文件内容')).toBeNull();
    expect(document.querySelector('[name="fileContent"]')).toBeNull();
  });

  it('点击编辑按钮打开的弹窗同样不含 fileContent 字段', async () => {
    renderComponent();
    await screen.findByText('知识文档.md');
    fireEvent.click(screen.getByText('编辑'));
    await waitFor(() => {
      expect(screen.getByLabelText('文件名')).toBeTruthy();
    });
    const nameInput = screen.getByLabelText('文件名') as HTMLInputElement;
    expect(nameInput.value).toBe('知识文档.md');
    expect(screen.queryByLabelText('文件内容')).toBeNull();
    expect(document.querySelector('[name="fileContent"]')).toBeNull();
  });

  it('源代码中弹窗 Form 不应出现 name="fileContent"', async () => {
    const source = readFileSync(resolve(__dirname, '../KnowledgeFileList.tsx'), 'utf-8');
    expect(source).toContain('name="fileName"');
    expect(source).toContain('name="fileDescription"');
    expect(source).not.toContain('name="fileContent"');
  });
});

describe('KnowledgeFileList 编辑内容按钮导航 (功能点2)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it('操作列应渲染「编辑内容」按钮', async () => {
    renderComponent();
    await screen.findByText('知识文档.md');
    expect(screen.getByText('编辑内容')).toBeTruthy();
  });

  it('点击「编辑内容」应导航到 /knowledge/kb-1/files/file-1/edit', async () => {
    renderComponent();
    await screen.findByText('知识文档.md');
    fireEvent.click(screen.getByText('编辑内容'));
    expect(mockNavigate).toHaveBeenCalledWith('/knowledge/kb-1/files/file-1/edit');
  });
});
