import { test, expect, Page } from '@playwright/test';
import { seedAdminLogin } from './utils/seedAuth';

const KB_ID = 'kb-100';
const FILE_ID = 'file-200';

const MOCK_CONTENT = '# 一级标题\n\n- 列表项一\n- 列表项二';

const MOCK_FILE = {
  id: FILE_ID,
  fileName: '知识文档.md',
  fileDescription: '测试文件描述',
  knowledgeBaseId: KB_ID,
  fileSize: 1024,
  lineCount: 5,
  status: 'ENABLED',
  createTime: '2026-08-01T00:00:00',
  updateTime: '2026-08-01T00:00:00',
};

const MOCK_FILES = [MOCK_FILE];

const MOCK_KB = {
  id: KB_ID,
  name: '测试知识库',
  description: '测试知识库描述',
  status: 'ENABLED',
  vectorModelId: null,
  esIndex: null,
  rebuilding: false,
  createTime: '2026-08-01T00:00:00',
  updateTime: '2026-08-01T00:00:00',
};

async function setupMocks(page: Page, opts?: { loadFail?: boolean; saveFail?: boolean }) {
  await page.route(`**/api/knowledge-bases/${KB_ID}`, async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: MOCK_KB }),
      });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
  });
  await page.route(`**/api/knowledge-bases/${KB_ID}/files/refresh`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: null }),
    });
  });
  await page.route(`**/api/knowledge-bases/${KB_ID}/files/${FILE_ID}/publish`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: null }),
    });
  });
  await page.route(`**/api/knowledge-bases/${KB_ID}/files`, async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: MOCK_FILES }),
      });
      return;
    }
    if (route.request().method() === 'POST') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: MOCK_FILE }),
      });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
  });
  await page.route(`**/api/knowledge-bases/${KB_ID}/files/${FILE_ID}`, async (route) => {
    if (opts?.loadFail) {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: '系统内部错误', data: null }),
      });
      return;
    }
    if (route.request().method() === 'PUT') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: MOCK_FILE }),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: MOCK_FILE }),
    });
  });
  await page.route(`**/api/knowledge-bases/${KB_ID}/files/${FILE_ID}/content`, async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: MOCK_CONTENT }),
      });
      return;
    }
    if (route.request().method() === 'PUT') {
      const ok = opts?.saveFail !== true;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: ok,
          message: ok ? '操作成功' : '系统内部错误',
          data: null,
        }),
      });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
  });
}

test.beforeEach(async ({ page }) => {
  await seedAdminLogin(page);
});

test.describe('知识文件列表页 - 编辑内容按钮与弹窗字段', () => {
  test('操作列应渲染「编辑内容」按钮', async ({ page }) => {
    await setupMocks(page);
    await page.goto(`/knowledge/${KB_ID}/files`);
    await page.waitForSelector('.ant-table');
    await expect(page.locator('.ant-table-tbody tr[data-row-key]').first().getByText('编辑内容')).toBeVisible();
  });

  test('点击「编辑内容」应跳转到 /knowledge/:kbId/files/:fileId/edit', async ({ page }) => {
    await setupMocks(page);
    await page.goto(`/knowledge/${KB_ID}/files`);
    await page.waitForSelector('.ant-table');
    await page.locator('.ant-table-tbody tr[data-row-key]').first().getByText('编辑内容').click();
    await page.waitForURL(`**/knowledge/${KB_ID}/files/${FILE_ID}/edit`);
    await expect(page.getByRole('button', { name: /保\s*存/ })).toBeVisible();
  });

  test('返回按钮应渲染文案为「返回」', async ({ page }) => {
    await setupMocks(page);
    await page.goto(`/knowledge/${KB_ID}/files`);
    await page.waitForSelector('.ant-table');
    await expect(page.getByRole('button', { name: '返回' })).toBeVisible();
    await expect(page.getByRole('button', { name: '返回知识库管理' })).toHaveCount(0);
  });

  test('点击返回按钮应导航到 /knowledge', async ({ page }) => {
    await setupMocks(page);
    await page.route('**/api/knowledge-bases', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] }),
      });
    });
    await page.goto(`/knowledge/${KB_ID}/files`);
    await page.waitForSelector('.ant-table');
    await page.getByRole('button', { name: '返回' }).click();
    await page.waitForURL('**/knowledge');
  });

  test('新增文件弹窗仅包含文件名与描述字段，不含文件内容字段', async ({ page }) => {
    await setupMocks(page);
    await page.goto(`/knowledge/${KB_ID}/files`);
    await page.waitForSelector('.ant-table');
    await page.getByRole('button', { name: '新增文件' }).click();
    await page.locator('.ant-modal').waitFor();
    await expect(page.locator('.ant-modal').getByLabel('文件名')).toBeVisible();
    await expect(page.locator('.ant-modal').getByLabel('描述')).toBeVisible();
    await expect(page.locator('.ant-modal').getByLabel('文件内容')).toHaveCount(0);
  });
});

test.describe('知识文件列表页 - 发布状态与发布/刷新按钮', () => {
  test('MOCK_FILE 无 publishStatus 时按 UNPUBLISHED 展示「未发布」Tag', async ({ page }) => {
    await setupMocks(page);
    await page.goto(`/knowledge/${KB_ID}/files`);
    await page.waitForSelector('.ant-table');
    await expect(page.locator('.ant-table').getByText('未发布')).toBeVisible();
  });

  test('点击「发布」应调用 POST publish 并提示「发布成功」', async ({ page }) => {
    await setupMocks(page);
    await page.goto(`/knowledge/${KB_ID}/files`);
    await page.waitForSelector('.ant-table');
    const [request] = await Promise.all([
      page.waitForRequest(
        (req) =>
          req.method() === 'POST' &&
          req.url().includes(`/api/knowledge-bases/${KB_ID}/files/${FILE_ID}/publish`),
      ),
      page.locator('.ant-table-tbody tr[data-row-key]').first().getByRole('button', { name: '发布' }).click(),
    ]);
    expect(request).toBeTruthy();
    await expect(
      page.locator('.ant-message-notice-content:has-text("发布成功")'),
    ).toBeVisible();
  });

  test('点击「刷新」应调用 PUT refresh 并重新拉取文件列表', async ({ page }) => {
    await setupMocks(page);
    await page.goto(`/knowledge/${KB_ID}/files`);
    await page.waitForSelector('.ant-table');
    const [request] = await Promise.all([
      page.waitForRequest(
        (req) =>
          req.method() === 'PUT' &&
          req.url().includes(`/api/knowledge-bases/${KB_ID}/files/refresh`),
      ),
      page.getByRole('button', { name: /刷\s*新/ }).click(),
    ]);
    expect(request).toBeTruthy();
    await expect(
      page.locator('.ant-message-notice-content:has-text("文件列表已刷新")'),
    ).toBeVisible();
  });
});

test.describe('知识文件内容编辑页 - 发布中禁用编辑', () => {
  test('文件 publishStatus=PUBLISHING 时 TextArea 与保存按钮禁用并显示发布中 Tag', async ({ page }) => {
    await setupMocks(page);
    await page.route(`**/api/knowledge-bases/${KB_ID}/files/${FILE_ID}`, async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { ...MOCK_FILE, publishStatus: 'PUBLISHING' },
          }),
        });
        return;
      }
      await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
    });
    await page.goto(`/knowledge/${KB_ID}/files/${FILE_ID}/edit`);
    await expect(page.locator('text=发布中，暂不可编辑')).toBeVisible();
    await expect(page.locator('textarea')).toBeDisabled();
    await expect(page.getByRole('button', { name: /保\s*存/ })).toBeDisabled();
  });
});

test.describe('知识文件内容编辑页', () => {
  test('挂载时通过内容接口加载文件内容，左侧 TextArea 显示内容', async ({ page }) => {
    await setupMocks(page);
    await page.goto(`/knowledge/${KB_ID}/files/${FILE_ID}/edit`);
    const textarea = page.locator('textarea');
    await expect(textarea).toHaveValue('# 一级标题\n\n- 列表项一\n- 列表项二');
    await expect(page.locator('text=知识文档.md')).toBeVisible();
  });

  test('加载失败应提示「获取文件详情失败」', async ({ page }) => {
    await setupMocks(page, { loadFail: true });
    await page.goto(`/knowledge/${KB_ID}/files/${FILE_ID}/edit`);
    await expect(
      page.locator('.ant-message-notice-content:has-text("获取文件详情失败")'),
    ).toBeVisible();
  });

  test('内容为空时右侧显示「预览区域」占位', async ({ page }) => {
    await setupMocks(page);
    await page.route(`**/api/knowledge-bases/${KB_ID}/files/${FILE_ID}/content`, async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: '' }),
        });
        return;
      }
      await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
    });
    await page.goto(`/knowledge/${KB_ID}/files/${FILE_ID}/edit`);
    await expect(page.locator('text=预览区域')).toBeVisible();
  });

  test('编辑 Markdown 后右侧实时预览渲染结果', async ({ page }) => {
    await setupMocks(page);
    await page.goto(`/knowledge/${KB_ID}/files/${FILE_ID}/edit`);
    const textarea = page.locator('textarea');
    await expect(textarea).toHaveValue('# 一级标题\n\n- 列表项一\n- 列表项二');
    await page.locator('text=列表项一').first().waitFor();

    await textarea.fill('# 新标题\n\n**加粗文本**');
    await expect(page.locator('h1')).toHaveText('新标题');
    await expect(page.locator('strong')).toHaveText('加粗文本');
  });

  test('点击保存应调用 PUT 更新内容并提示「保存成功」', async ({ page }) => {
    await setupMocks(page);
    await page.goto(`/knowledge/${KB_ID}/files/${FILE_ID}/edit`);
    const textarea = page.locator('textarea');
    await expect(textarea).toHaveValue('# 一级标题\n\n- 列表项一\n- 列表项二');

    const [request] = await Promise.all([
      page.waitForRequest(
        (req) =>
          req.method() === 'PUT' &&
          req.url().includes(`/api/knowledge-bases/${KB_ID}/files/${FILE_ID}/content`),
      ),
      page.getByRole('button', { name: /保\s*存/ }).click(),
    ]);
    expect(request.postData()).toBe('# 一级标题\n\n- 列表项一\n- 列表项二');
    await expect(
      page.locator('.ant-message-notice-content:has-text("保存成功")'),
    ).toBeVisible();
  });

  test('保存失败应提示「保存失败」', async ({ page }) => {
    await setupMocks(page, { saveFail: true });
    await page.goto(`/knowledge/${KB_ID}/files/${FILE_ID}/edit`);
    const textarea = page.locator('textarea');
    await expect(textarea).toHaveValue('# 一级标题\n\n- 列表项一\n- 列表项二');
    await page.getByRole('button', { name: /保\s*存/ }).click();
    await expect(
      page.locator('.ant-message-notice-content:has-text("保存失败")'),
    ).toBeVisible();
  });

  test('点击「关闭」应返回文件列表页', async ({ page }) => {
    await setupMocks(page);
    await page.goto(`/knowledge/${KB_ID}/files/${FILE_ID}/edit`);
    await page.waitForSelector('textarea');
    await page.getByRole('button', { name: /关\s*闭/ }).click();
    await page.waitForURL(`**/knowledge/${KB_ID}/files`);
    await expect(page.locator('.ant-table')).toBeVisible();
  });

  test('编辑页不应存在「返回文件列表」按钮', async ({ page }) => {
    await setupMocks(page);
    await page.goto(`/knowledge/${KB_ID}/files/${FILE_ID}/edit`);
    await page.waitForSelector('textarea');
    await expect(page.getByRole('button', { name: '返回文件列表' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: /关\s*闭/ })).toBeVisible();
  });
});
