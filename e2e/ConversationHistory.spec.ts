import { test, expect, Page } from '@playwright/test';

test.describe.configure({ timeout: 120000 });

const MOCK_SESSIONS = [
  { id: 'session-1', agentId: 'agent-1', modelId: 'gpt-4', title: '测试会话1', createTime: '2026-07-11T03:00:00Z', updateTime: '2026-07-11T03:30:00Z' },
];

const MOCK_USER_MESSAGES = [
  { id: 'um-1', sessionId: 'session-1', conversationId: 'conv-1', role: 'user', content: '用户消息一', sequenceNum: 1, createTime: '2026-07-11T03:01:00Z' },
  { id: 'um-2', sessionId: 'session-1', conversationId: 'conv-2', role: 'user', content: '用户消息二', sequenceNum: 2, createTime: '2026-07-11T03:02:00Z' },
];

const MOCK_DETAIL_MESSAGES = [
  { id: 'dm-1', sessionId: 'session-1', conversationId: 'conv-1', role: 'user', content: '用户消息一', sequenceNum: 1, createTime: '2026-07-11T03:01:00Z' },
  { id: 'dm-2', sessionId: 'session-1', conversationId: 'conv-1', role: 'assistant', content: '助手回复一', sequenceNum: 2, createTime: '2026-07-11T03:01:05Z' },
];

const LONG_SESSION_ID = 'abcdefgh-1234-5678-90ab-cdef01234567';
const LONG_SESSION_SHORT = 'abcdefgh…4567';

const MOCK_TOOL_DETAIL_MESSAGES = [
  { id: 'td-1', sessionId: 'session-1', conversationId: 'conv-1', role: 'assistant', content: '助手调用工具', sequenceNum: 1, createTime: '2026-07-11T03:01:00Z', toolCalls: [{ toolCallId: 'tc-1', toolCallName: 'search', toolCallArguments: '{}' }, { toolCallId: 'tc-2', toolCallName: 'read', toolCallArguments: '{}' }] },
  { id: 'td-2', sessionId: 'session-1', conversationId: 'conv-1', role: 'tool', content: '工具执行结果', sequenceNum: 2, createTime: '2026-07-11T03:01:05Z', toolResult: '{"result": "ok"}' },
  { id: 'td-3', sessionId: 'session-1', conversationId: 'conv-1', role: 'assistant', content: '最终回复', sequenceNum: 3, createTime: '2026-07-11T03:01:10Z' },
];

const MOCK_CHILD_MESSAGES = [
  { id: 'cd-1', sessionId: LONG_SESSION_ID, conversationId: 'conv-1', role: 'user', content: '子会话消息', sequenceNum: 1, createTime: '2026-07-11T03:02:00Z' },
  { id: 'cd-2', sessionId: 'session-1', conversationId: 'conv-1', role: 'user', content: '主会话消息', sequenceNum: 2, createTime: '2026-07-11T03:03:00Z' },
];

async function setupToolMocks(page: Page) {
  await page.route('**/api/conversations/conv-1/messages', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_TOOL_DETAIL_MESSAGES }) });
  });
}

async function setupRowMocks(page: Page) {
  await page.route('**/api/sessions/session-1/messages', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_USER_MESSAGES }) });
  });
  await page.route('**/api/conversations/conv-1/messages', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_CHILD_MESSAGES }) });
  });
}

async function setupHistoryMocks(page: Page) {
  await page.route('**/api/sessions', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_SESSIONS }) });
  });
  await page.route('**/api/sessions/session-1/messages', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_USER_MESSAGES }) });
  });
  await page.route('**/api/conversations/conv-1/messages', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_DETAIL_MESSAGES }) });
  });
}

test.describe('会话历史返回与跳转优化', () => {
  test('用户消息列表点击「查看详情」应跳转到详情页', async ({ page }) => {
    await setupHistoryMocks(page);
    await page.goto('/conversations/session-1');
    await page.waitForSelector('.ant-table');

    await page.locator('text=查看详情').first().click();
    await page.waitForURL('**/conversations/conv-1/detail');

    await expect(page.locator('text=对话详情')).toBeVisible();
    await expect(page.locator('text=助手回复一')).toBeVisible();
  });

  test('详情页带 sessionId 时「返回」应回到 /conversations/:sessionId', async ({ page }) => {
    await setupHistoryMocks(page);
    await page.goto('/conversations/session-1');
    await page.waitForSelector('.ant-table');

    await page.locator('text=查看详情').first().click();
    await page.waitForURL('**/conversations/conv-1/detail');

    await page.locator('text=返回').first().click();
    await page.waitForURL('**/conversations/session-1');
  });

  test('详情页无 sessionId 时「返回」应回到 /conversations', async ({ page }) => {
    await setupHistoryMocks(page);
    await page.goto('/conversations/conv-1/detail');
    await page.waitForSelector('.ant-table');

    await page.locator('text=返回').first().click();
    await page.waitForURL('**/conversations');
    await expect(page.getByRole('heading', { name: '会话历史' })).toBeVisible();
  });

  test('用户消息列表「返回」按钮文案为「返回」并导航到 /conversations', async ({ page }) => {
    await setupHistoryMocks(page);
    await page.goto('/conversations/session-1');
    await page.waitForSelector('.ant-table');

    const backBtn = page.locator('text=返回').first();
    await expect(backBtn).toBeVisible();
    await expect(page.locator('text=返回会话列表')).toHaveCount(0);

    await backBtn.click();
    await page.waitForURL('**/conversations');
    await expect(page.getByRole('heading', { name: '会话历史' })).toBeVisible();
  });

  test('用户消息列表应展示用户消息并含「查看详情」', async ({ page }) => {
    await setupHistoryMocks(page);
    await page.goto('/conversations/session-1');
    await page.waitForSelector('.ant-table');

    await expect(page.locator('text=用户消息一')).toBeVisible();
    await expect(page.locator('text=用户消息二')).toBeVisible();
    await expect(page.locator('text=查看详情').first()).toBeVisible();
  });
});

test.describe('对话详情工具按钮与来源会话', () => {
  test('assistant 有 toolCalls 时应显示 🔧 工具调用按钮和数量', async ({ page }) => {
    await setupToolMocks(page);
    await page.goto('/conversations/conv-1/detail');
    await page.waitForSelector('.ant-table');

    await expect(page.locator('text=工具调用 (2)')).toBeVisible();
    await expect(page.locator('text=助手调用工具')).toBeVisible();
  });

  test('点击 assistant 内容应弹出「对话详情」Modal 展示工具调用', async ({ page }) => {
    await setupToolMocks(page);
    await page.goto('/conversations/conv-1/detail');
    await page.waitForSelector('.ant-table');

    await page.locator('.ant-table-tbody tr').first().click();
    await expect(page.locator('.ant-modal-content')).toBeVisible();
    await expect(page.locator('.ant-modal-title')).toHaveText('对话详情');
    await expect(page.locator('.ant-modal-content')).toContainText('🔧 工具调用');
    await expect(page.locator('.ant-modal-content')).toContainText('search');
    await expect(page.locator('.ant-modal-content')).toContainText('read');
  });

  test('Modal 应展示 tool 消息工具结果 JSON', async ({ page }) => {
    await setupToolMocks(page);
    await page.goto('/conversations/conv-1/detail');
    await page.waitForSelector('.ant-table');

    await page.locator('.ant-table-tbody tr').first().click();
    await expect(page.locator('.ant-modal-content')).toBeVisible();
    await expect(page.locator('.ant-modal-content')).toContainText('📋 工具结果');
    await expect(page.locator('.ant-modal-content')).toContainText('{"result": "ok"}');
  });

  test('长 sessionId 应截短显示为前8后4加省略号', async ({ page }) => {
    await setupRowMocks(page);
    await page.goto('/conversations/conv-1/detail');
    await page.waitForSelector('.ant-table');

    await expect(page.locator(`text=${LONG_SESSION_SHORT}`)).toBeVisible();
    await expect(page.locator(`text=${LONG_SESSION_ID}`)).toHaveCount(0);
  });

  test('Tooltip 悬浮应显示完整 sessionId', async ({ page }) => {
    await setupRowMocks(page);
    await page.goto('/conversations/conv-1/detail');
    await page.waitForSelector('.ant-table');

    await page.locator(`text=${LONG_SESSION_SHORT}`).hover();
    await expect(page.locator('.ant-tooltip-inner')).toContainText(LONG_SESSION_ID);
  });

  test('rowClassName 应区分主会话与子会话行背景', async ({ page }) => {
    await setupRowMocks(page);
    await page.goto('/conversations/session-1');
    await page.waitForSelector('.ant-table');
    await page.locator('text=查看详情').first().click();
    await page.waitForURL('**/conversations/conv-1/detail');
    await page.waitForSelector('.ant-table');

    const mainRow = page.locator('tr.conversation-main-row');
    const childRow = page.locator('tr.conversation-child-row');
    await expect(mainRow).toHaveCount(1);
    await expect(childRow).toHaveCount(1);
  });
});
