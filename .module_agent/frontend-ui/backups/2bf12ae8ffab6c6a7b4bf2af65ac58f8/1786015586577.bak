import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { KnowledgeBase, KnowledgeFile } from '../../../types/knowledge';

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
  listKnowledgeFiles: vi.fn(),
  getKnowledgeBase: vi.fn(),
  publishKnowledgeFile: vi.fn(),
  refreshKnowledgeFiles: vi.fn(),
  createKnowledgeFile: vi.fn(),
  updateKnowledgeFile: vi.fn(),
  deleteKnowledgeFile: vi.fn(),
  updateKnowledgeFileStatus: vi.fn(),
};

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ kbId: 'kb-1' }),
  };
});

vi.mock('../../../services/knowledge', () => ({
  listKnowledgeFiles: (...args: unknown[]) => mocks.listKnowledgeFiles(...args),
  getKnowledgeBase: (...args: unknown[]) => mocks.getKnowledgeBase(...args),
  publishKnowledgeFile: (...args: unknown[]) => mocks.publishKnowledgeFile(...args),
  refreshKnowledgeFiles: (...args: unknown[]) => mocks.refreshKnowledgeFiles(...args),
  createKnowledgeFile: (...args: unknown[]) => mocks.createKnowledgeFile(...args),
  updateKnowledgeFile: (...args: unknown[]) => mocks.updateKnowledgeFile(...args),
  deleteKnowledgeFile: (...args: unknown[]) => mocks.deleteKnowledgeFile(...args),
  updateKnowledgeFileStatus: (...args: unknown[]) => mocks.updateKnowledgeFileStatus(...args),
}));

import KnowledgeFileList from '../KnowledgeFileList';

function makeFile(overrides: Partial<KnowledgeFile> = {}): KnowledgeFile {
  return {
    id: 'file-1',
    fileName: '知识文档.md',
    fileDescription: '测试文件',
    knowledgeBaseId: 'kb-1',
    status: 'ENABLED',
    publishStatus: 'UNPUBLISHED',
    createTime: '2026-08-01T00:00:00',
    updateTime: '2026-08-01T00:00:00',
    ...overrides,
  };
}

function makeKb(overrides: Partial<KnowledgeBase> = {}): KnowledgeBase {
  return {
    id: 'kb-1',
    name: '知识库',
    status: 'ENABLED',
    rebuilding: false,
    createTime: '2026-08-01T00:00:00',
    updateTime: '2026-08-01T00:00:00',
    ...overrides,
  };
}

function renderComponent() {
  return render(
    <MemoryRouter>
      <KnowledgeFileList />
    </MemoryRouter>,
  );
}

function getTagByText(text: string): HTMLElement | null {
  const tags = Array.from(document.querySelectorAll('.ant-tag')) as HTMLElement[];
  return tags.find((t) => t.textContent === text) ?? null;
}

describe('KnowledgeFileList 发布状态 Tag 列 (功能点2)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mocks.listKnowledgeFiles.mockReset();
    mocks.getKnowledgeBase.mockReset();
    mocks.publishKnowledgeFile.mockReset();
    mocks.refreshKnowledgeFiles.mockReset();
    mocks.getKnowledgeBase.mockResolvedValue(makeKb());
  });

  it('应渲染 5 种发布状态文案', async () => {
    mocks.listKnowledgeFiles.mockResolvedValue([
      makeFile({ id: 'f1', publishStatus: 'UNPUBLISHED' }),
      makeFile({ id: 'f2', publishStatus: 'PUBLISHING' }),
      makeFile({ id: 'f3', publishStatus: 'PUBLISHED' }),
      makeFile({ id: 'f4', publishStatus: 'PENDING_PUBLISH' }),
      makeFile({ id: 'f5', publishStatus: 'PUBLISH_ERROR' }),
    ]);
    renderComponent();
    await waitFor(() => {
      expect(getTagByText('未发布')).toBeTruthy();
    });
    expect(getTagByText('发布中')).toBeTruthy();
    expect(getTagByText('已发布')).toBeTruthy();
    expect(getTagByText('待发布')).toBeTruthy();
    expect(getTagByText('发布失败')).toBeTruthy();
  });

  it('各状态 Tag 颜色应正确 (processing/success/warning/error)', async () => {
    mocks.listKnowledgeFiles.mockResolvedValue([
      makeFile({ id: 'f2', publishStatus: 'PUBLISHING' }),
      makeFile({ id: 'f3', publishStatus: 'PUBLISHED' }),
      makeFile({ id: 'f4', publishStatus: 'PENDING_PUBLISH' }),
      makeFile({ id: 'f5', publishStatus: 'PUBLISH_ERROR' }),
    ]);
    renderComponent();
    await waitFor(() => {
      expect(getTagByText('发布中')?.className).toContain('ant-tag-processing');
    });
    expect(getTagByText('已发布')?.className).toContain('ant-tag-success');
    expect(getTagByText('待发布')?.className).toContain('ant-tag-warning');
    expect(getTagByText('发布失败')?.className).toContain('ant-tag-error');
  });

  it('publishStatus 缺失时按 UNPUBLISHED 展示（未发布）', async () => {
    const file = makeFile();
    delete (file as Partial<KnowledgeFile>).publishStatus;
    mocks.listKnowledgeFiles.mockResolvedValue([file]);
    renderComponent();
    await screen.findByText('未发布');
    const tag = getTagByText('未发布');
    expect(tag).toBeTruthy();
    expect(tag?.className).not.toContain('ant-tag-processing');
  });
});

describe('KnowledgeFileList 发布按钮逻辑 (功能点3)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mocks.listKnowledgeFiles.mockReset();
    mocks.getKnowledgeBase.mockReset();
    mocks.publishKnowledgeFile.mockReset();
    mocks.refreshKnowledgeFiles.mockReset();
    mocks.getKnowledgeBase.mockResolvedValue(makeKb());
  });

  it('PUBLISHING 状态发布按钮文案为「发布中」且禁用', async () => {
    mocks.listKnowledgeFiles.mockResolvedValue([
      makeFile({ id: 'f1', publishStatus: 'PUBLISHING' }),
    ]);
    renderComponent();
    const btn = await screen.findByRole('button', { name: '发布中' });
    expect((btn as HTMLButtonElement).disabled).toBe(true);
  });

  it('PUBLISHED 状态发布按钮禁用', async () => {
    mocks.listKnowledgeFiles.mockResolvedValue([
      makeFile({ id: 'f1', publishStatus: 'PUBLISHED' }),
    ]);
    renderComponent();
    await screen.findByText('已发布');
    const btn = screen.getByRole('button', { name: '发布' }) as HTMLButtonElement;
    expect(btn.disabled).toBe(true);
  });

  it.each(['UNPUBLISHED', 'PENDING_PUBLISH', 'PUBLISH_ERROR'] as const)(
    '%s 状态发布按钮可点击',
    async (status) => {
      mocks.listKnowledgeFiles.mockResolvedValue([
        makeFile({ id: 'f1', publishStatus: status }),
      ]);
      renderComponent();
      await screen.findByText('发布');
      const btn = screen.getByRole('button', { name: '发布' }) as HTMLButtonElement;
      expect(btn.disabled).toBe(false);
    },
  );

  it('知识库 rebuilding=true 时发布按钮禁用', async () => {
    mocks.listKnowledgeFiles.mockResolvedValue([
      makeFile({ id: 'f1', publishStatus: 'UNPUBLISHED' }),
    ]);
    mocks.getKnowledgeBase.mockResolvedValue(makeKb({ rebuilding: true }));
    renderComponent();
    await screen.findByText('发布');
    const btn = screen.getByRole('button', { name: '发布' }) as HTMLButtonElement;
    expect(btn.disabled).toBe(true);
  });

  it('点击发布调用 publishKnowledgeFile(kbId, fileId) 并提示「发布成功」', async () => {
    mocks.listKnowledgeFiles.mockResolvedValue([
      makeFile({ id: 'f1', publishStatus: 'UNPUBLISHED' }),
    ]);
    mocks.publishKnowledgeFile.mockResolvedValue(undefined);
    renderComponent();
    const btn = await screen.findByRole('button', { name: '发布' });
    fireEvent.click(btn);
    await waitFor(() => {
      expect(mocks.publishKnowledgeFile).toHaveBeenCalledWith('kb-1', 'f1');
    });
    await waitFor(() => {
      expect(document.body.textContent).toContain('发布成功');
    });
  });

  it('发布失败应提示「发布失败」', async () => {
    mocks.listKnowledgeFiles.mockResolvedValue([
      makeFile({ id: 'f1', publishStatus: 'UNPUBLISHED' }),
    ]);
    mocks.publishKnowledgeFile.mockRejectedValue(new Error('失败'));
    renderComponent();
    const btn = await screen.findByRole('button', { name: '发布' });
    fireEvent.click(btn);
    await waitFor(() => {
      expect(document.body.textContent).toContain('发布失败');
    });
  });
});

describe('KnowledgeFileList 刷新按钮 (功能点4)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mocks.listKnowledgeFiles.mockReset();
    mocks.getKnowledgeBase.mockReset();
    mocks.publishKnowledgeFile.mockReset();
    mocks.refreshKnowledgeFiles.mockReset();
    mocks.getKnowledgeBase.mockResolvedValue(makeKb());
    mocks.refreshKnowledgeFiles.mockResolvedValue(undefined);
  });

  it('点击刷新调用 refreshKnowledgeFiles(kbId) 并重新拉取文件列表', async () => {
    mocks.listKnowledgeFiles.mockResolvedValue([makeFile({ id: 'f1' })]);
    renderComponent();
    await screen.findByText('知识文档.md');
    fireEvent.click(screen.getByRole('button', { name: /刷\s*新/ }));
    await waitFor(() => {
      expect(mocks.refreshKnowledgeFiles).toHaveBeenCalledWith('kb-1');
    });
    await waitFor(() => {
      expect(mocks.listKnowledgeFiles).toHaveBeenCalledTimes(2);
    });
    await waitFor(() => {
      expect(document.body.textContent).toContain('文件列表已刷新');
    });
  });
});
