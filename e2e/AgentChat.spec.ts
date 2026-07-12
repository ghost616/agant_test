import { test, expect, Page, Route } from '@playwright/test';

const MOCK_SESSION = {
  id: 'session-1',
  agentId: 'agent-1',
  modelId: 'gpt-4',
  title: '测试会话',
  systemPrompt: 'You are a helpful assistant',
  parentSessionId: undefined,
  isChild: false,
  createTime: '2026-07-11T03:00:00Z',
  updateTime: '2026-07-11T03:30:00Z',
};

const MOCK_MESSAGES = [
  { id: 'msg-1', sessionId: 'session-1', role: 'user', content: '你好', sequenceNum: 1, createTime: '2026-07-11T03:01:00Z' },
  { id: 'msg-2', sessionId: 'session-1', role: 'assistant', content: '你好！有什么可以帮助你的？', reasoning: '思考中...', sequenceNum: 2, createTime: '2026-07-11T03:01:05Z' },
];

const MOCK_CHILD_SESSIONS = [
  { id: 'child-1', agentId: 'agent-1', modelId: 'gpt-4', title: '子会话1', isChild: true, parentSessionId: 'session-1', createTime: '2026-07-11T03:10:00Z', updateTime: '2026-07-11T03:20:00Z' },
  { id: 'child-2', agentId: 'agent-1', modelId: 'gpt-4', title: '子会话2', isChild: true, parentSessionId: 'session-1', createTime: '2026-07-11T03:15:00Z', updateTime: '2026-07-11T03:25:00Z' },
];

const MOCK_CHILD_MESSAGES = [
  { id: 'cmsg-1', sessionId: 'child-1', role: 'user', content: '子会话问题', sequenceNum: 1, createTime: '2026-07-11T03:11:00Z' },
  { id: 'cmsg-2', sessionId: 'child-1', role: 'assistant', content: '子会话回答', sequenceNum: 2, createTime: '2026-07-11T03:11:05Z' },
];

async function setupMocks(page: Page, childSessions = MOCK_CHILD_SESSIONS, childMessages = MOCK_CHILD_MESSAGES) {
  await page.route('**/api/sessions/session-1', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_SESSION }) });
  });
  await page.route('**/api/sessions/session-1/messages', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_MESSAGES }) });
  });
  await page.route('**/api/sessions/session-1/children', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: childSessions }) });
  });
  await page.route('**/api/sessions/child-1/messages', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: childMessages }) });
  });
  await page.route('**/api/models*', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: [] }) });
  });
}

test.describe('AgentChat Tab 切换与子会话只读查看', () => {
  test('应展示「主会话」和「子会话列表」两个 Tab', async ({ page }) => {
    await setupMocks(page);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    const tabs = page.locator('.ant-tabs-tab');
    await expect(tabs).toHaveCount(2);
    await expect(tabs.nth(0)).toHaveText('主会话');
    await expect(tabs.nth(1)).toHaveText('子会话列表');
  });

  test('「主会话」Tab 应包含完整的聊天界面组件', async ({ page }) => {
    await setupMocks(page);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    await expect(page.getByPlaceholder('输入消息，Enter 发送，Shift+Enter 换行')).toBeVisible();
    await expect(page.locator('.ant-select')).toBeVisible();
    await expect(page.locator('.ant-switch')).toBeVisible();
  });

  test('切换到「子会话列表」Tab 应显示子会话列表', async ({ page }) => {
    await setupMocks(page);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    await page.locator('.ant-tabs-tab').nth(1).click();
    await page.waitForSelector('.ant-table');

    const rows = page.locator('.ant-table-tbody tr');
    await expect(rows).toHaveCount(2);
  });

  test('子会话列表为空时应显示「暂无子会话」', async ({ page }) => {
    await setupMocks(page, []);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    await page.locator('.ant-tabs-tab').nth(1).click();
    await expect(page.locator('text=暂无子会话')).toBeVisible();
  });

  test('子会话列表应包含「查看会话」按钮', async ({ page }) => {
    await setupMocks(page);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    await page.locator('.ant-tabs-tab').nth(1).click();
    await page.waitForSelector('.ant-table');

    await expect(page.locator('text=查看会话').first()).toBeVisible();
  });

  test('点击「查看会话」应加载子会话历史消息', async ({ page }) => {
    await setupMocks(page);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    await page.locator('.ant-tabs-tab').nth(1).click();
    await page.waitForSelector('.ant-table');
    await page.locator('text=查看会话').first().click();
    await page.waitForTimeout(500);

    await expect(page.locator('text=子会话问题')).toBeVisible();
    await expect(page.locator('text=子会话回答')).toBeVisible();
  });

  test('子会话查看模式下应显示「返回子会话列表」按钮', async ({ page }) => {
    await setupMocks(page);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    await page.locator('.ant-tabs-tab').nth(1).click();
    await page.waitForSelector('.ant-table');
    await page.locator('text=查看会话').first().click();
    await page.waitForTimeout(500);

    await expect(page.locator('text=返回子会话列表')).toBeVisible();
  });

  test('子会话查看模式下不应显示消息输入框、模型选择器和思考模式开关', async ({ page }) => {
    await setupMocks(page);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    await page.locator('.ant-tabs-tab').nth(1).click();
    await page.waitForSelector('.ant-table');
    await page.locator('text=查看会话').first().click();
    await page.waitForTimeout(500);

    await expect(page.getByPlaceholder('输入消息，Enter 发送，Shift+Enter 换行')).not.toBeVisible();
    await expect(page.locator('.ant-select')).not.toBeVisible();
    await expect(page.locator('.ant-switch')).not.toBeVisible();
  });
});


