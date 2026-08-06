import { describe, it, expect, vi, beforeEach } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { KnowledgeFile } from '../../../types/knowledge';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ kbId: 'kb-1', fileId: 'file-1' }),
  };
});

const MOCK_FILE: KnowledgeFile = {
  id: 'file-1',
  fileName: '知识文档.md',
  fileDescription: '测试文件',
  knowledgeBaseId: 'kb-1',
  status: 'ENABLED',
  fileContent: '# 标题\n\n这是 **加粗** 内容',
  createTime: '2026-08-01T00:00:00',
  updateTime: '2026-08-01T00:00:00',
};

const mocks = {
  getKnowledgeFile: vi.fn(),
  updateKnowledgeFile: vi.fn(),
};

vi.mock('../../../services/knowledge', () => ({
  getKnowledgeFile: (...args: unknown[]) => mocks.getKnowledgeFile(...args),
  updateKnowledgeFile: (...args: unknown[]) => mocks.updateKnowledgeFile(...args),
}));

import KnowledgeFileEdit from '../KnowledgeFileEdit';

function renderComponent() {
  return render(
    <MemoryRouter>
      <KnowledgeFileEdit />
    </MemoryRouter>,
  );
}

describe('KnowledgeFileEdit 加载文件详情 (功能点3)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mocks.getKnowledgeFile.mockReset();
    mocks.updateKnowledgeFile.mockReset();
  });

  it('挂载时应按 kbId/fileId 调用 getKnowledgeFile', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    renderComponent();
    await waitFor(() => {
      expect(mocks.getKnowledgeFile).toHaveBeenCalledWith('kb-1', 'file-1');
    });
  });

  it('加载成功后应用 fileContent 填充左侧 TextArea', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    renderComponent();
    await waitFor(() => {
      const textarea = document.querySelector('textarea') as HTMLTextAreaElement | null;
      expect(textarea).toBeTruthy();
      expect(textarea!.value).toBe('# 标题\n\n这是 **加粗** 内容');
    });
  });

  it('加载成功应显示文件名', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    renderComponent();
    expect(await screen.findByText('知识文档.md')).toBeTruthy();
  });

  it('加载失败应提示「获取文件详情失败」', async () => {
    mocks.getKnowledgeFile.mockRejectedValue(new Error('网络异常'));
    renderComponent();
    await waitFor(() => {
      expect(document.querySelector('.ant-message-notice-content')).toBeTruthy();
    });
    expect(document.body.textContent).toContain('获取文件详情失败');
  });
});

describe('KnowledgeFileEdit 左右分栏与实时预览 (功能点4)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mocks.getKnowledgeFile.mockReset();
    mocks.updateKnowledgeFile.mockReset();
  });

  it('内容为空时应显示「预览区域」占位', async () => {
    mocks.getKnowledgeFile.mockResolvedValue({ ...MOCK_FILE, fileContent: '' });
    renderComponent();
    expect(await screen.findByText('预览区域')).toBeTruthy();
  });

  it('左侧 TextArea 输入内容后右侧应实时预览 Markdown 渲染', async () => {
    mocks.getKnowledgeFile.mockResolvedValue({ ...MOCK_FILE, fileContent: '' });
    renderComponent();
    await waitFor(() => expect(screen.getByPlaceholderText('请输入 Markdown 内容')).toBeTruthy());

    const textarea = screen.getByPlaceholderText('请输入 Markdown 内容');
    fireEvent.change(textarea, { target: { value: '# 新标题' } });

    await waitFor(() => {
      expect(document.querySelector('h1')).toBeTruthy();
      expect(screen.getByText('新标题')).toBeTruthy();
    });
  });

  it('应使用 react-markdown 与 remark-gfm 插件', () => {
    const source = readFileSync(resolve(__dirname, '../KnowledgeFileEdit.tsx'), 'utf-8');
    expect(source).toContain('ReactMarkdown');
    expect(source).toContain('remarkGfm');
    expect(source).toContain('remarkPlugins={[remarkGfm]}');
  });
});

describe('KnowledgeFileEdit 保存逻辑 (功能点5)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mocks.getKnowledgeFile.mockReset();
    mocks.updateKnowledgeFile.mockReset();
  });

  it('点击保存应调用 updateKnowledgeFile(kbId, fileId, { fileContent })', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    mocks.updateKnowledgeFile.mockResolvedValue(MOCK_FILE);
    renderComponent();
    await waitFor(() => expect(screen.getByRole('textbox')).toBeTruthy());
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }));
    await waitFor(() => {
      expect(mocks.updateKnowledgeFile).toHaveBeenCalledWith('kb-1', 'file-1', {
        fileContent: '# 标题\n\n这是 **加粗** 内容',
      });
    });
  });

  it('保存成功应提示「保存成功」', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    mocks.updateKnowledgeFile.mockResolvedValue(MOCK_FILE);
    renderComponent();
    await waitFor(() => expect(screen.getByRole('textbox')).toBeTruthy());
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }));
    await waitFor(() => {
      expect(document.body.textContent).toContain('保存成功');
    });
  });

  it('保存失败应提示「保存失败」', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    mocks.updateKnowledgeFile.mockRejectedValue(new Error('保存出错'));
    renderComponent();
    await waitFor(() => expect(screen.getByRole('textbox')).toBeTruthy());
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }));
    await waitFor(() => {
      expect(document.body.textContent).toContain('保存失败');
    });
  });

  it('保存中按钮应进入 loading 状态', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    let resolveSave!: (v: KnowledgeFile) => void;
    mocks.updateKnowledgeFile.mockImplementation(
      () =>
        new Promise<KnowledgeFile>((r) => {
          resolveSave = r;
        }),
    );
    renderComponent();
    await waitFor(() => expect(screen.getByRole('textbox')).toBeTruthy());

    const saveButton = screen.getByRole('button', { name: /保\s*存/ }) as HTMLButtonElement;
    fireEvent.click(saveButton);
    await waitFor(() => {
      expect(saveButton.className).toContain('ant-btn-loading');
    });
    resolveSave(MOCK_FILE);
    await waitFor(() => {
      expect(document.body.textContent).toContain('保存成功');
    });
  });
});

describe('KnowledgeFileEdit 关闭/返回导航 (功能点6)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mocks.getKnowledgeFile.mockReset();
    mocks.updateKnowledgeFile.mockReset();
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
  });

  it('点击「返回文件列表」应导航回 /knowledge/kb-1/files', async () => {
    renderComponent();
    fireEvent.click(await screen.findByText('返回文件列表'));
    expect(mockNavigate).toHaveBeenCalledWith('/knowledge/kb-1/files');
  });

  it('点击「关闭」应导航回 /knowledge/kb-1/files', async () => {
    renderComponent();
    await screen.findByText('返回文件列表');
    fireEvent.click(screen.getByRole('button', { name: /关\s*闭/ }));
    expect(mockNavigate).toHaveBeenCalledWith('/knowledge/kb-1/files');
  });
});
