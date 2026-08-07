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
  createTime: '2026-08-01T00:00:00',
  updateTime: '2026-08-01T00:00:00',
};

const MOCK_CONTENT = '# 标题\n\n这是 **加粗** 内容';

const mocks = {
  getKnowledgeFile: vi.fn(),
  getKnowledgeFileContent: vi.fn(),
  updateKnowledgeFileContent: vi.fn(),
};

vi.mock('../../../services/knowledge', () => ({
  getKnowledgeFile: (...args: unknown[]) => mocks.getKnowledgeFile(...args),
  getKnowledgeFileContent: (...args: unknown[]) =>
    mocks.getKnowledgeFileContent(...args),
  updateKnowledgeFileContent: (...args: unknown[]) =>
    mocks.updateKnowledgeFileContent(...args),
}));

import KnowledgeFileEdit from '../KnowledgeFileEdit';

function renderComponent() {
  return render(
    <MemoryRouter>
      <KnowledgeFileEdit />
    </MemoryRouter>,
  );
}

describe('KnowledgeFileEdit 加载文件内容 (功能点3)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mocks.getKnowledgeFile.mockReset();
    mocks.getKnowledgeFileContent.mockReset();
    mocks.updateKnowledgeFileContent.mockReset();
  });

  it('挂载时应按 kbId/fileId 调用 getKnowledgeFile 与 getKnowledgeFileContent', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    mocks.getKnowledgeFileContent.mockResolvedValue(MOCK_CONTENT);
    renderComponent();
    await waitFor(() => {
      expect(mocks.getKnowledgeFile).toHaveBeenCalledWith('kb-1', 'file-1');
      expect(mocks.getKnowledgeFileContent).toHaveBeenCalledWith('kb-1', 'file-1');
    });
  });

  it('加载成功后应用 getKnowledgeFileContent 返回内容填充左侧 TextArea', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    mocks.getKnowledgeFileContent.mockResolvedValue(MOCK_CONTENT);
    renderComponent();
    await waitFor(() => {
      const textarea = document.querySelector('textarea') as HTMLTextAreaElement | null;
      expect(textarea).toBeTruthy();
      expect(textarea!.value).toBe('# 标题\n\n这是 **加粗** 内容');
    });
  });

  it('加载成功应显示文件名', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    mocks.getKnowledgeFileContent.mockResolvedValue(MOCK_CONTENT);
    renderComponent();
    expect(await screen.findByText('知识文档.md')).toBeTruthy();
  });

  it('getKnowledgeFileContent 返回 null 时不应抛错，TextArea 内容为空字符串', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    mocks.getKnowledgeFileContent.mockResolvedValue(null as unknown as string);
    renderComponent();
    await waitFor(() => {
      const textarea = document.querySelector('textarea') as HTMLTextAreaElement | null;
      expect(textarea).toBeTruthy();
      expect(textarea!.value).toBe('');
    });
    expect(document.body.textContent).not.toContain('获取文件详情失败');
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
    mocks.getKnowledgeFileContent.mockReset();
    mocks.updateKnowledgeFileContent.mockReset();
  });

  it('内容为空时应显示「预览区域」占位', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    mocks.getKnowledgeFileContent.mockResolvedValue('');
    renderComponent();
    expect(await screen.findByText('预览区域')).toBeTruthy();
  });

  it('左侧 TextArea 输入内容后右侧应实时预览 Markdown 渲染', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    mocks.getKnowledgeFileContent.mockResolvedValue('');
    renderComponent();
    await waitFor(() =>
      expect(screen.getByPlaceholderText('请输入 Markdown 内容')).toBeTruthy(),
    );

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

  it('左侧输入框与右侧预览区应使用相同高度设置', () => {
    const source = readFileSync(resolve(__dirname, '../KnowledgeFileEdit.tsx'), 'utf-8');
    expect(source).toContain('height: EDITOR_HEIGHT');
    expect(source).toContain('EDITOR_HEIGHT = 640');
  });
});

describe('KnowledgeFileEdit 保存逻辑 (功能点5)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mocks.getKnowledgeFile.mockReset();
    mocks.getKnowledgeFileContent.mockReset();
    mocks.updateKnowledgeFileContent.mockReset();
  });

  it('点击保存应调用 updateKnowledgeFileContent(kbId, fileId, content)', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    mocks.getKnowledgeFileContent.mockResolvedValue(MOCK_CONTENT);
    mocks.updateKnowledgeFileContent.mockResolvedValue(undefined);
    renderComponent();
    await waitFor(() => expect(screen.getByRole('textbox')).toBeTruthy());
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }));
    await waitFor(() => {
      expect(mocks.updateKnowledgeFileContent).toHaveBeenCalledWith(
        'kb-1',
        'file-1',
        '# 标题\n\n这是 **加粗** 内容',
      );
    });
  });

  it('保存成功应提示「保存成功」', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    mocks.getKnowledgeFileContent.mockResolvedValue(MOCK_CONTENT);
    mocks.updateKnowledgeFileContent.mockResolvedValue(undefined);
    renderComponent();
    await waitFor(() => expect(screen.getByRole('textbox')).toBeTruthy());
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }));
    await waitFor(() => {
      expect(document.body.textContent).toContain('保存成功');
    });
  });

  it('保存失败应提示「保存失败」', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    mocks.getKnowledgeFileContent.mockResolvedValue(MOCK_CONTENT);
    mocks.updateKnowledgeFileContent.mockRejectedValue(new Error('保存出错'));
    renderComponent();
    await waitFor(() => expect(screen.getByRole('textbox')).toBeTruthy());
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }));
    await waitFor(() => {
      expect(document.body.textContent).toContain('保存失败');
    });
  });

  it('保存中按钮应进入 loading 状态', async () => {
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    mocks.getKnowledgeFileContent.mockResolvedValue(MOCK_CONTENT);
    let resolveSave!: (v: unknown) => void;
    mocks.updateKnowledgeFileContent.mockImplementation(
      () =>
        new Promise((r) => {
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
    resolveSave(undefined);
    await waitFor(() => {
      expect(document.body.textContent).toContain('保存成功');
    });
  });
});

describe('KnowledgeFileEdit 关闭导航 (功能点6)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mocks.getKnowledgeFile.mockReset();
    mocks.getKnowledgeFileContent.mockReset();
    mocks.updateKnowledgeFileContent.mockReset();
    mocks.getKnowledgeFile.mockResolvedValue(MOCK_FILE);
    mocks.getKnowledgeFileContent.mockResolvedValue(MOCK_CONTENT);
  });

  it('点击「关闭」应导航回 /knowledge/kb-1/files', async () => {
    renderComponent();
    await screen.findByText('知识文档.md');
    fireEvent.click(screen.getByRole('button', { name: /关\s*闭/ }));
    expect(mockNavigate).toHaveBeenCalledWith('/knowledge/kb-1/files');
  });

  it('页面不应渲染「返回文件列表」按钮', async () => {
    renderComponent();
    await screen.findByText('知识文档.md');
    expect(screen.queryByText('返回文件列表')).toBeNull();
  });

  it('保存/关闭按钮区应使用右下角对齐 (flex-end)', () => {
    const source = readFileSync(resolve(__dirname, '../KnowledgeFileEdit.tsx'), 'utf-8');
    expect(source).toContain("justifyContent: 'flex-end'");
  });
});
