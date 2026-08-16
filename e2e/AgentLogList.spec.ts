import { test, expect, Page } from '@playwright/test';
import { seedAdminLogin } from './utils/seedAuth';

const LONG_DATA = JSON.stringify({ message: '这是一条非常长的日志内容，用于触发展开按钮'.repeat(20) });

const MOCK_SESSIONS = [
  {
    id: '100',
    agentId: 'agent-1',
    modelId: 'model-1',
    title: '主会话A',
    parentSessionId: null,
    isChild: false,
    isEvaluation: false,
    createTime: '2026-08-16 10:00:00',
    updateTime: '2026-08-16 10:00:00',
  },
  {
    id: '300',
    agentId: 'agent-2',
    modelId: 'model-2',
    title: '评估会话C',
    parentSessionId: null,
    isChild: false,
    isEvaluation: true,
    createTime: '2026-08-16 09:00:00',
    updateTime: '2026-08-16 09:00:00',
  },
];

const MOCK_LOGS = [
  {
    id: '1',
    sessionId: '100',
    sessionName: '主会话A',
    conversationId: 'conv-1',
    logType: 'MODEL_CALL',
    logLevel: 'INFO',
    logData: LONG_DATA,
    isChild: false,
    createTime: '2026-08-09 10:00:00',
  },
  {
    id: '2',
    sessionId: '200',
    sessionName: '子会话B',
    conversationId: 'conv-2',
    logType: 'ERROR_LOG',
    logLevel: 'ERROR',
    logData: '{"a":1}',
    isChild: true,
    createTime: '2026-08-09 09:00:00',
  },
];

async function setupMocks(page: Page) {
  await page.route('**/api/sessions/log-sessions', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        code: 'SYS-000',
        message: '操作成功',
        data: MOCK_SESSIONS,
      }),
    });
  });
  await page.route('**/api/agent-logs**', async (route) => {
    const url = new URL(route.request().url());
    const pageNum = Number(url.searchParams.get('page') || '1');
    const size = Number(url.searchParams.get('size') || '20');
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        code: 'SYS-000',
        message: '操作成功',
        data: { list: MOCK_LOGS, total: 100, page: pageNum, size },
      }),
    });
  });
}

test.beforeEach(async ({ page }) => {
  await seedAdminLogin(page);
});

test.describe('会话日志与运行日志页面', () => {
  test.beforeEach(async ({ page }) => {
    await setupMocks(page);
  });

  test('/logs 展示会话日志表格（会话名/是否评估/创建时间/操作）', async ({ page }) => {
    await page.goto('/logs');
    await page.waitForSelector('.ant-table');

    await expect(page.locator('.ant-menu').getByText('运行日志')).toBeVisible();

    const headerTexts = await page.locator('.ant-table-thead th').allTextContents();
    for (const title of ['会话名', '是否评估', '创建时间', '操作']) {
      expect(headerTexts.some((t) => t.includes(title))).toBe(true);
    }

    await expect(page.locator('.ant-table-tbody').getByText('主会话A')).toBeVisible();
    // exact 匹配避免与标题「评估会话C」等包含子串的单元格冲突（strict mode）
    await expect(page.locator('.ant-table-tbody').getByText('评估会话', { exact: true })).toBeVisible();
    await expect(page.locator('.ant-table-tbody').getByText('普通会话', { exact: true }).first()).toBeVisible();
  });

  test('点击查看日志跳转 /logs/{sessionId} 并渲染运行日志表格（含会话类型列）', async ({ page }) => {
    await page.goto('/logs');
    await page.waitForSelector('.ant-table');

    await page
      .locator('.ant-table-tbody tr', { hasText: '主会话A' })
      .getByRole('button', { name: '查看日志' })
      .click();
    await expect(page).toHaveURL(/\/logs\/100$/);
    await page.waitForSelector('.ant-table');

    const headerTexts = await page.locator('.ant-table-thead th').allTextContents();
    for (const title of [
      '会话名',
      '会话类型',
      '对话ID',
      '日志类型',
      '日志等级',
      '日志数据',
      '创建时间',
    ]) {
      expect(headerTexts.some((t) => t.includes(title))).toBe(true);
    }

    await expect(page.locator('.ant-table-tbody').getByText('主会话A')).toBeVisible();
    await expect(page.locator('.ant-table-tbody').getByText('子会话B')).toBeVisible();
    await expect(
      page.locator('.ant-table-tbody .ant-tag', { hasText: '子会话' }).first(),
    ).toBeVisible();
    await expect(
      page.locator('.ant-table-tbody .ant-tag', { hasText: '主会话' }).first(),
    ).toBeVisible();
  });

  test('日志类型显示中文 Tag，日志等级显示彩色字体', async ({ page }) => {
    await page.goto('/logs/100');
    await page.waitForSelector('.ant-table');

    const modelCallTag = page.locator('.ant-table-tbody .ant-tag', { hasText: '模型调用' });
    await expect(modelCallTag).toBeVisible();

    const firstRow = page.locator('.ant-table-tbody tr[data-row-key]').first();
    const infoSpan = firstRow.locator('span', { hasText: 'INFO' });
    await expect(infoSpan).toBeVisible();
    await expect(infoSpan).toHaveCSS('color', 'rgb(22, 119, 255)');

    const secondRow = page.locator('.ant-table-tbody tr[data-row-key]').nth(1);
    const errorSpan = secondRow.locator('span', { hasText: 'ERROR' });
    await expect(errorSpan).toBeVisible();
    await expect(errorSpan).toHaveCSS('color', 'rgb(255, 77, 79)');
  });

  test('点击展开按钮弹出日志详情 Modal 展示完整数据', async ({ page }) => {
    await page.goto('/logs/100');
    await page.waitForSelector('.ant-table');

    const expandBtn = page
      .locator('.ant-table-tbody tr[data-row-key]')
      .first()
      .getByRole('button', { name: '展开' });
    await expect(expandBtn).toBeVisible();
    await expandBtn.click();

    await expect(page.locator('.ant-modal .ant-modal-title')).toHaveText('日志详情');
    const modalContent = await page.locator('.ant-modal').textContent();
    expect(modalContent).toContain('非常长的日志内容');
    expect(modalContent).toContain('主会话A');
  });

  test('关闭日志详情 Modal 后再次展开仍可正常打开', async ({ page }) => {
    await page.goto('/logs/100');
    await page.waitForSelector('.ant-table');

    const expandBtn = page
      .locator('.ant-table-tbody tr[data-row-key]')
      .first()
      .getByRole('button', { name: '展开' });
    await expandBtn.click();
    await expect(page.locator('.ant-modal .ant-modal-title')).toHaveText('日志详情');

    await page.locator('.ant-modal-footer .ant-btn').click();
    await expect(page.locator('.ant-modal .ant-modal-title')).toHaveCount(0);

    await expandBtn.click();
    await expect(page.locator('.ant-modal .ant-modal-title')).toHaveText('日志详情');
    const modalContent = await page.locator('.ant-modal').textContent();
    expect(modalContent).toContain('非常长的日志内容');
  });

  test('点击下一页触发带 page=2 的重新请求（携带 rootSessionId）', async ({ page }) => {
    await page.goto('/logs/100');
    await page.waitForSelector('.ant-table');

    const [request] = await Promise.all([
      page.waitForRequest(
        (req) => req.url().includes('/api/agent-logs') && req.url().includes('page=2'),
      ),
      page.locator('.ant-pagination-item-2').click(),
    ]);
    expect(request.url()).toContain('/api/agent-logs');
    expect(request.url()).toContain('rootSessionId=100');
  });

  test('选择日志类型筛选后触发带 logType 的重新请求（携带 rootSessionId）', async ({ page }) => {
    await page.goto('/logs/100');
    await page.waitForSelector('.ant-table');

    const logTypeSelect = page.locator('.ant-select').filter({ hasText: '日志类型' });
    await logTypeSelect.click();

    const [request] = await Promise.all([
      page.waitForRequest(
        (req) => req.url().includes('/api/agent-logs') && req.url().includes('logType=MODEL_CALL'),
      ),
      page
        .locator('.ant-select-dropdown .ant-select-item-option', { hasText: '模型调用' })
        .click(),
    ]);
    expect(request.url()).toContain('logType=MODEL_CALL');
    expect(request.url()).toContain('rootSessionId=100');
  });
});
