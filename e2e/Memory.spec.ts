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

  await page.route('**/api/sessions/*/messages/range*', async (route) => {
    const url = new URL(route.request().url());
    const startSeq = url.searchParams.get('startSeq');
    const endSeq = url.searchParams.get('endSeq');
    const ranged = MOCK_MESSAGES.filter(
      (m) => m.sequenceNum >= Number(startSeq) && m.sequenceNum <= Number(endSeq),
    );
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: okJson(ranged),
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

  await page.route('**/api/sessions/*/memory-prompt', async (route) => {
    if (route.request().method() === 'PUT') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: okJson(null),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: okJson('记忆聚合提示语'),
    });
  });

  await page.route('**/api/sessions/*/memory/regenerate/status*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: okJson({
        sessionId: '1',
        docId: '1_DAILY_1_5',
        status: 'COMPLETED',
        aggregationText: '重生成后的每日摘要',
      }),
    });
  });

  await page.route('**/api/sessions/*/memory/regenerate', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: okJson({
        sessionId: '1',
        docId: '1_DAILY_1_5',
        status: 'RUNNING',
      }),
    });
  });

  await page.route('**/api/sessions/*/memory/update', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: okJson(null),
    });
  });

  await page.route('**/api/models*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: okJson([
        {
          id: 'm1',
          name: 'LLM模型',
          modelType: 'LLM',
        },
      ]),
    });
  });
}

test.describe('记忆修改 MemoryList 页面', () => {
  test.describe.configure({ timeout: 120000 });
  test.beforeEach(async ({ page }) => {
    await setupMocks(page);
  });

  test('/memory 路由可访问，侧边栏存在记忆修改菜单，仅展示 memoryEnabled 智能体会话', async ({ page }) => {
    await page.goto('/memory');
    await page.waitForSelector('.ant-table');

    await expect(page.locator('.ant-menu').getByText('记忆修改')).toBeVisible();

    await expect(page.locator('.ant-table-tbody').getByText('记忆会话')).toBeVisible();
    await expect(page.locator('.ant-table-tbody').getByText('普通会话')).toHaveCount(0);
  });

  test('表格渲染智能体名与最近消息时间（末条消息 createTime）', async ({ page }) => {
    await page.goto('/memory');
    await page.waitForSelector('.ant-table');

    await expect(page.locator('.ant-table-tbody').getByText('记忆智能体')).toBeVisible();
    await expect(page.locator('.ant-table-tbody').getByText('普通智能体')).toHaveCount(0);
    await expect(
      page.locator('.ant-table-tbody').getByText('2026-08-10 10:00:00'),
    ).toBeVisible();

    const headerTexts = await page.locator('.ant-table-thead th').allTextContents();
    for (const title of ['会话名称', '智能体名', '最近消息时间', '操作']) {
      expect(headerTexts.some((t) => t.includes(title))).toBe(true);
    }
  });

  test('点击按日聚合跳转 /memory/1/DAILY 并展示聚合日期列', async ({ page }) => {
    await page.goto('/memory');
    await page.waitForSelector('.ant-table');

    await page
      .locator('.ant-table-tbody')
      .getByRole('button', { name: '按日聚合' })
      .click();

    await page.waitForURL('**/memory/1/DAILY');
    await expect(page.getByText('按日聚合记忆')).toBeVisible();

    const headerTexts = await page.locator('.ant-table-thead th').allTextContents();
    expect(headerTexts.some((t) => t.includes('聚合日期'))).toBe(true);
    await expect(page.locator('.ant-table-tbody').getByText('每日摘要')).toBeVisible();
  });

  test('点击按分类聚合跳转 /memory/1/GROUP 并展示起始-结束列', async ({ page }) => {
    await page.goto('/memory');
    await page.waitForSelector('.ant-table');

    await page
      .locator('.ant-table-tbody')
      .getByRole('button', { name: '按分类聚合' })
      .click();

    await page.waitForURL('**/memory/1/GROUP');
    await expect(page.getByText('按分类聚合记忆')).toBeVisible();

    const headerTexts = await page.locator('.ant-table-thead th').allTextContents();
    expect(headerTexts.some((t) => t.includes('起始-结束'))).toBe(true);
    await expect(page.locator('.ant-table-tbody').getByText('1 - 3')).toBeVisible();
    await expect(page.locator('.ant-table-tbody').getByText('分组摘要')).toBeVisible();
  });
});

test.describe('记忆修改 MemoryDetail 页面', () => {
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

  test('聚合列表详情按钮跳转详情页并携带 startSeq/endSeq 参数', async ({ page }) => {
    await page.goto('/memory/1/DAILY');
    await page.waitForSelector('.ant-table');

    const [request] = await Promise.all([
      page.waitForRequest(
        (req) =>
          req.url().includes('/api/sessions/1/messages/range') &&
          req.url().includes('startSeq=1') &&
          req.url().includes('endSeq=5'),
      ),
      page.locator('.ant-table-tbody').getByRole('button', { name: '详情' }).click(),
    ]);

    await page.waitForURL('**/memory/1/DAILY/1-5');
    expect(request.url()).toContain('/api/sessions/1/messages/range');
  });

  test('GROUP 详情按钮跳转 /memory/1/GROUP/1-3', async ({ page }) => {
    await page.goto('/memory/1/GROUP');
    await page.waitForSelector('.ant-table');

    await page
      .locator('.ant-table-tbody')
      .getByRole('button', { name: '详情' })
      .click();

    await page.waitForURL('**/memory/1/GROUP/1-3');
    await expect(page.getByText('聚合文档详情')).toBeVisible();
  });
});

test.describe('记忆修改 MemoryDocumentDetail 页面', () => {
  test.describe.configure({ timeout: 120000 });
  test.beforeEach(async ({ page }) => {
    await setupMocks(page);
  });

  test('左侧只读展示聚合文本，右侧以对话气泡展示生成消息与角色标签', async ({ page }) => {
    await page.goto('/memory/1/DAILY');
    await page.waitForSelector('.ant-table');

    await page
      .locator('.ant-table-tbody')
      .getByRole('button', { name: '详情' })
      .click();
    await page.waitForURL('**/memory/1/DAILY/1-5');

    const textarea = page.locator('textarea').first();
    await expect(textarea).toBeVisible();
    await expect(textarea).toHaveValue('每日摘要');
    await expect(textarea).toHaveAttribute('readonly', '');

    await expect(page.locator('.agent-chat-markdown').getByText('hi')).toBeVisible();
    await expect(page.locator('.agent-chat-markdown').getByText('hello')).toBeVisible();
    await expect(page.getByText('你')).toBeVisible();
    await expect(page.getByText('助手')).toBeVisible();
  });

  test('返回按钮跳转 /memory/:sessionId/:type', async ({ page }) => {
    await page.goto('/memory/1/DAILY');
    await page.waitForSelector('.ant-table');

    await page
      .locator('.ant-table-tbody')
      .getByRole('button', { name: '详情' })
      .click();
    await page.waitForURL('**/memory/1/DAILY/1-5');

    await page.getByRole('button', { name: '返回' }).click();
    await page.waitForURL('**/memory/1/DAILY');
  });

  test('配置按钮打开弹窗仅展示已保存提示语（无模型下拉），保存调用 PUT memory-prompt', async ({ page }) => {
    await page.goto('/memory/1/DAILY');
    await page.waitForSelector('.ant-table');
    await page
      .locator('.ant-table-tbody')
      .getByRole('button', { name: '详情' })
      .click();
    await page.waitForURL('**/memory/1/DAILY/1-5');

    await page.getByRole('button', { name: /配\s*置/ }).click();
    await expect(page.getByText('提示语配置')).toBeVisible();
    await expect(page.locator('.ant-modal textarea').first()).toHaveValue('记忆聚合提示语');
    await expect(page.locator('.ant-modal .ant-select-selector')).toHaveCount(0);

    const [request] = await Promise.all([
      page.waitForRequest(
        (req) =>
          req.url().includes('/api/sessions/1/memory-prompt') &&
          req.method() === 'PUT',
      ),
      page.locator('.ant-modal').getByRole('button', { name: /保\s*存/ }).click(),
    ]);
    expect(request.postDataJSON()).toEqual({ prompt: '记忆聚合提示语' });
  });

  test('编辑按钮打开弹窗，展示模型下拉，保存调用 POST memory/update', async ({ page }) => {
    await page.goto('/memory/1/DAILY');
    await page.waitForSelector('.ant-table');
    await page
      .locator('.ant-table-tbody')
      .getByRole('button', { name: '详情' })
      .click();
    await page.waitForURL('**/memory/1/DAILY/1-5');

    await page.getByRole('button', { name: /编\s*辑/ }).click();
    await expect(page.getByText('编辑聚合文档')).toBeVisible();
    await expect(page.locator('.ant-modal textarea').nth(1)).toHaveValue('每日摘要');

    await page.locator('.ant-modal .ant-select-selector').click();
    await expect(page.locator('.ant-select-dropdown:visible').getByText('LLM模型')).toBeVisible();
    await page.keyboard.press('Escape');

    const [request] = await Promise.all([
      page.waitForRequest(
        (req) =>
          req.url().includes('/api/sessions/1/memory/update') &&
          req.method() === 'POST',
      ),
      page.locator('.ant-modal').getByRole('button', { name: /保\s*存/ }).click(),
    ]);
    expect(request.postDataJSON()).toEqual({
      docId: '1_DAILY_1_5',
      text: '每日摘要',
    });
  });

  test('重新生成按钮调用 regenerate 并轮询 status，完成后回填右侧输入框', async ({ page }) => {
    await page.goto('/memory/1/DAILY');
    await page.waitForSelector('.ant-table');
    await page
      .locator('.ant-table-tbody')
      .getByRole('button', { name: '详情' })
      .click();
    await page.waitForURL('**/memory/1/DAILY/1-5');

    await page.getByRole('button', { name: /编\s*辑/ }).click();
    await expect(page.getByText('编辑聚合文档')).toBeVisible();

    const [request] = await Promise.all([
      page.waitForRequest(
        (req) =>
          req.url().includes('/api/sessions/1/memory/regenerate') &&
          req.method() === 'POST',
      ),
      page.getByRole('button', { name: '重新生成' }).click(),
    ]);
    expect(request.postDataJSON()).toEqual({
      docId: '1_DAILY_1_5',
      startSeq: 1,
      endSeq: 5,
      prompt: '记忆聚合提示语',
    });

    await expect(page.locator('.ant-modal textarea').nth(1)).toHaveValue(
      '重生成后的每日摘要',
    );
  });
});
