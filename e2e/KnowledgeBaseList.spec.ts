import { test, expect, Page } from '@playwright/test';
import { seedAdminLogin } from './utils/seedAuth';

const KB_ID = 'kb-100';

const MOCK_KB = {
  id: KB_ID,
  name: '测试知识库',
  description: '测试知识库描述',
  status: 'ENABLED',
  vectorModelId: 'vm-1',
  esIndex: 'es-index-1',
  rebuilding: false,
  createTime: '2026-08-01T00:00:00',
  updateTime: '2026-08-01T00:00:00',
};

const MOCK_KB_NO_ES = {
  ...MOCK_KB,
  id: 'kb-101',
  name: '无ES索引知识库',
  esIndex: null,
};

const MOCK_MODEL = {
  id: 'vm-1',
  name: '向量模型A',
  platformType: 'openai',
  modelName: 'embed-1',
};

async function setupMocks(page: Page) {
  await page.route('**/api/knowledge-bases', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [MOCK_KB] }),
      });
      return;
    }
    if (route.request().method() === 'POST') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: MOCK_KB }),
      });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
  });
  await page.route(`**/api/knowledge-bases/${KB_ID}`, async (route) => {
    if (route.request().method() === 'PUT') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: MOCK_KB }),
      });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
  });
  await page.route('**/api/models', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: [MOCK_MODEL] }),
    });
  });
}

test.beforeEach(async ({ page }) => {
  await seedAdminLogin(page);
});

test.describe('知识库管理页 - ES 索引列与提交排除', () => {
  test.setTimeout(120000);
  test.beforeEach(async ({ page }) => {
    await setupMocks(page);
    await page.goto('/knowledge', { waitUntil: 'domcontentloaded', timeout: 120000 });
    await page.waitForSelector('.ant-table', { timeout: 60000 });
  });

  test('表格渲染「ES 索引」列，有值时显示 esIndex', async ({ page }) => {
    const thead = page.locator('.ant-table-thead');
    await expect(thead.getByText('ES 索引')).toBeVisible();
    await expect(page.getByRole('cell', { name: 'es-index-1' })).toBeVisible();
  });

  test('esIndex 为空时表格「ES 索引」列显示 -', async ({ page }) => {
    await page.route('**/api/knowledge-bases', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: [MOCK_KB_NO_ES] }),
        });
        return;
      }
      await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
    });
    await page.reload({ waitUntil: 'domcontentloaded' });
    await page.waitForSelector('.ant-table');
    await expect(page.getByRole('row', { name: /无ES索引知识库/ })).toContainText('-');
  });

  test('新增弹窗中不包含 ES 索引输入框', async ({ page }) => {
    await page.getByRole('button', { name: '新增知识库' }).click();
    await page.locator('.ant-modal').waitFor();
    await expect(page.locator('.ant-modal').getByLabel('ES 索引')).toHaveCount(0);
  });

  test('编辑弹窗中不包含 ES 索引输入框（即使记录存在 esIndex）', async ({ page }) => {
    await page.getByRole('button', { name: '编辑' }).click();
    await page.locator('.ant-modal').waitFor();
    await expect(page.locator('.ant-modal').getByLabel('ES 索引')).toHaveCount(0);
  });

  test('新增提交时 POST payload 不应包含 esIndex 字段', async ({ page }) => {
    const [request] = await Promise.all([
      page.waitForRequest(
        (req) => req.method() === 'POST' && req.url().includes('/api/knowledge-bases'),
      ),
      (async () => {
        await page.getByRole('button', { name: '新增知识库' }).click();
        await page.locator('.ant-modal').waitFor();
        await page.locator('.ant-modal').getByLabel('名称').fill('新知识库');
        await page.locator('.ant-modal-footer .ant-btn-primary').click();
      })(),
    ]);
    const payload = request.postDataJSON();
    expect(payload).not.toHaveProperty('esIndex');
    expect(payload.name).toBe('新知识库');
  });

  test('编辑提交时 PUT payload 不应包含 esIndex 字段（即使记录存在该值）', async ({ page }) => {
    const [request] = await Promise.all([
      page.waitForRequest(
        (req) =>
          req.method() === 'PUT' && req.url().includes(`/api/knowledge-bases/${KB_ID}`),
      ),
      (async () => {
        await page.getByRole('button', { name: '编辑' }).click();
        await page.locator('.ant-modal').waitFor();
        await page.locator('.ant-modal').getByLabel('名称').fill('修改知识库');
        await page.locator('.ant-modal-footer .ant-btn-primary').click();
      })(),
    ]);
    const payload = request.postDataJSON();
    expect(payload).not.toHaveProperty('esIndex');
    expect(payload.name).toBe('修改知识库');
  });
});
