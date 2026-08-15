import { test, expect, Page } from '@playwright/test';
import { seedAdminLogin } from './utils/seedAuth';

test.describe.configure({ timeout: 120000 });

const MOCK_TOOLS = [
  { id: 'tool-1', name: 'test_tool', toolType: 'JAVA', description: '测试工具', parameterSchema: '', returnSchema: '', implPath: '/impl/test', status: 'ENABLED', createTime: '2026-07-11T03:00:00Z', updateTime: '2026-07-11T03:00:00Z' },
];

async function setupMocks(page: Page) {
  await page.route('**/api/tools', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_TOOLS }) });
  });
}

test.beforeEach(async ({ page }) => {
  await seedAdminLogin(page);
});

test.describe('ToolList JsonEditor 懒加载', () => {
  test('工具列表应正常渲染，不因懒加载崩溃', async ({ page }) => {
    await setupMocks(page);
    await page.goto('/tools');
    await page.waitForSelector('.ant-table');

    await expect(page.locator('text=测试工具')).toBeVisible();
    await expect(page.locator('text=test_tool')).toBeVisible();
  });

  test('新增工具 Modal 中 JsonEditor 懒加载后正常渲染编辑器', async ({ page }) => {
    await setupMocks(page);
    await page.goto('/tools');
    await page.waitForSelector('.ant-table');

    await page.locator('text=新增工具').click();
    await page.waitForSelector('.ant-modal-content');

    await expect(page.locator('text=参数 Schema')).toBeVisible();
    await expect(page.locator('text=返回 Schema')).toBeVisible();

    await expect(page.locator('.ant-modal-content .cm-editor').first()).toBeVisible({ timeout: 15000 });
  });
});
