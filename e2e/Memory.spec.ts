import { test, expect, Page } from '@playwright/test';

const MOCK_SESSIONS = [
  {
    id: '1',
    agentId: 'a1',
    modelId: 'm1',
    title: '记忆会话',
    createTime: '2026-08-01T00:00:00',
    updateTime: '2026-08-01T00:00:00',
  },
  {
    id: '2',
    agentId: 'a2',
    modelId: 'm2',
    title: '普通会话',
    createTime: '2026-08-01T00:00:00',
    updateTime: '2026-08-01T00:00:00',
  },
];

const MOCK_AGENTS = [
  {
    id: 'a1',
    name: '记忆智能体',
    status: 'ENABLED',
    tools: [],
    skills: [],
    createTime: '2026-08-01T00:00:00',
    updateTime: '2026-08-01T00:00:00',
    memoryEnabled: true,
  },
  {
    id: 'a2',
    name: '普通智能体',
    status: 'ENABLED',
    tools: [],
    skills: [],
    createTime: '2026-08-01T00:00:00',
    updateTime: '2026-08-01T00:00:00',
    memoryEnabled: false,
  },
];

const MOCK_MESSAGES = [
  {
    id: 'm1',
    sessionId: '1',
    role: 'user',
    content: 'hi',
    sequenceNum: 1,
    createTime: '2026-08-10 09:00:00',
  },
  {
    id: 'm2',
    sessionId: '1',
    role: 'assistant',
    content: 'hello',
    sequenceNum: 2,
    createTime: '2026-08-10 10:00:00',
  },
];

const MOCK_DAILY = {
  list: [
    {
      sessionId: '1',
      aggregationType: 'DAILY',
      aggregationStartSeq: 1,
      aggregationEndSeq: 5,
      aggregationStartTime: 1720000000000,
      aggregationEndTime: 1720000000000,
      aggregationText: '每日摘要',
    },
  ],
  total: 100,
  page: 1,
  size: 20,
};

const MOCK_GROUP = {
  list: [
    {
      sessionId: '1',
      aggregationType: 'GROUP',
      aggregationStartSeq: 1,
      aggregationEndSeq: 3,
      aggregationStartTime: 1720000000000,
      aggregationEndTime: 1720000000000,
      aggregationText: '分组摘要',
    },
  ],
  total: 100,
  page: 1,
  size: 20,
};

function okJson(data: unknown) {
  return JSON.stringify({
    success: true,
    code: 'SYS-000',
    message: '操作成功',
    data,
  });
}

async function setupMocks(page: Page) {
  await page.route('**/api/sessions', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: okJson(MOCK_SESSIONS),
    });
  });

  await page.route('**/api/agents', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: okJson(MOCK_AGENTS),
    });
  });

  await page.route('**/api/sessions/*/messages', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: okJson(MOCK_MESSAGES),
    });
  });

  await page.route('**/api/sessions/*/memory*', async (route) => {
    const url = new URL(route.request().url());
    const type = url.searchParams.get('type');
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: okJson(type === 'GROUP' ? MOCK_GROUP : MOCK_DAILY),
    });
  });
}

test.describe('记忆回看 MemoryList 页面', () => {
  test.describe.configure({ timeout: 120000 });
  test.beforeEach(async ({ page }) => {
    await setupMocks(page);
  });

  test('/memory 路由可访问，侧边栏存在记忆回看菜单，仅展示 memoryEnabled 智能体会话', async ({ page }) => {
    await page.goto('/memory');
    await page.waitForSelector('.ant-table');

    await expect(page.locator('.ant-menu').getByText('记忆回看')).toBeVisible();

    await expect(page.locator('.ant-table-tbody').getByText('记忆会话')).toBeVisible();
    await expect(page.locator('.ant-table-tbody').getByText('普通会话')).toHaveCount(0);
  });

  test('表格渲染智能体名与最近消息时间（末条消息 createTime）', async ({ page }) => {
    await page.goto('/memory');
    await page.waitForSelector('.ant-table');

    await expect(page.locator('.ant-table-tbody').getByText('记忆智能体')).toBeVisible();
    await expect(page.locator('.ant-table-tbody').getByText('2026-08-10 10:00:00')).toBeVisible();

    const headerTexts = await page.locator('.ant-table-thead th').allTextContents();
    for (const title of ['会话名称', '智能体名', '最近消息时间', '操作']) {
      expect(headerTexts.some((t) => t.includes(title))).toBe(true);
    }
  });

  test('点击按日聚合跳转 /memory/1/DAILY 并展示聚合日期列', async ({ page }) => {
    await page.goto('/memory');
    await page.waitForSelector('.ant-table');

    await page.locator('.ant-table-tbody').getByRole('button', { name: '按日聚合' }).click();

    await page.waitForURL('**/memory/1/DAILY');
    await expect(page.getByText('按日聚合记忆')).toBeVisible();

    const headerTexts = await page.locator('.ant-table-thead th').allTextContents();
    expect(headerTexts.some((t) => t.includes('聚合日期'))).toBe(true);
    await expect(page.locator('.ant-table-tbody').getByText('每日摘要')).toBeVisible();
  });

  test('点击按分类聚合跳转 /memory/1/GROUP 并展示起始行-结束行列', async ({ page }) => {
    await page.goto('/memory');
    await page.waitForSelector('.ant-table');

    await page.locator('.ant-table-tbody').getByRole('button', { name: '按分类聚合' }).click();

    await page.waitForURL('**/memory/1/GROUP');
    await expect(page.getByText('按分类聚合记忆')).toBeVisible();

    const headerTexts = await page.locator('.ant-table-thead th').allTextContents();
    expect(headerTexts.some((t) => t.includes('起始行-结束行'))).toBe(true);
    await expect(page.locator('.ant-table-tbody').getByText('1 - 3')).toBeVisible();
    await expect(page.locator('.ant-table-tbody').getByText('分组摘要')).toBeVisible();
  });
});

test.describe('记忆回看 MemoryDetail 页面', () => {
  test.describe.configure({ timeout: 120000 });
  test.beforeEach(async ({ page }) => {
    await setupMocks(page);
  });

  test('分页显示共 N 条，切换页码触发 page=2 请求', async ({ page }) => {
    await page.goto('/memory/1/DAILY');
    await page.waitForSelector('.ant-table');

    await expect(page.locator('.ant-pagination-total-text')).toHaveText('共 100 条');

    const [request] = await Promise.all([
      page.waitForRequest(
        (req) =>
          req.url().includes('/api/sessions/1/memory') && req.url().includes('page=2'),
      ),
      page.locator('.ant-pagination-item-2').click(),
    ]);
    expect(request.url()).toContain('type=DAILY');
  });

  test('返回按钮跳转 /memory', async ({ page }) => {
    await page.goto('/memory/1/DAILY');
    await page.waitForSelector('.ant-table');

    await page.getByRole('button', { name: '返回' }).click();
    await page.waitForURL('**/memory');
  });
});
