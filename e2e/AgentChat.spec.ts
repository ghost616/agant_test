import { test, expect, Page } from '@playwright/test';
import { seedAdminLogin } from './utils/seedAuth';

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

const MOCK_CHILD_WITHOUT_TITLE = [
  { id: 'child-3', agentId: 'agent-1', modelId: 'gpt-4', title: '', isChild: true, parentSessionId: 'session-1', createTime: '2026-07-11T03:10:00Z', updateTime: '2026-07-11T03:20:00Z' },
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

test.beforeEach(async ({ page }) => {
  await seedAdminLogin(page);
});

test.describe('AgentChat 子会话标签化展示', () => {
  test('应展示「主会话」标签和每个子会话标签', async ({ page }) => {
    await setupMocks(page);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    const tabs = page.locator('.ant-tabs-tab');
    await expect(tabs).toHaveCount(3);
    await expect(tabs.nth(0)).toHaveText('主会话');
    await expect(tabs.nth(1)).toHaveText('子会话1');
    await expect(tabs.nth(2)).toHaveText('子会话2');
  });

  test('子会话无 title 时标签显示子会话 id', async ({ page }) => {
    await setupMocks(page, MOCK_CHILD_WITHOUT_TITLE);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    const tabs = page.locator('.ant-tabs-tab');
    await expect(tabs).toHaveCount(2);
    await expect(tabs.nth(1)).toHaveText('child-3');
  });

  test('无子会话时仅显示「主会话」一个标签', async ({ page }) => {
    await setupMocks(page, []);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    const tabs = page.locator('.ant-tabs-tab');
    await expect(tabs).toHaveCount(1);
    await expect(tabs.nth(0)).toHaveText('主会话');
  });

  test('进入页面自动加载子会话标签，无需手动刷新', async ({ page }) => {
    await setupMocks(page);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    await expect(page.locator('.ant-tabs-tab').nth(1)).toHaveText('子会话1');
    await expect(page.locator('.ant-tabs-tab').nth(2)).toHaveText('子会话2');
  });

  test('「主会话」Tab 应包含完整的聊天界面组件', async ({ page }) => {
    await setupMocks(page);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    await expect(page.getByPlaceholder('输入消息，Enter 发送，Shift+Enter 换行')).toBeVisible();
    await expect(page.locator('.ant-select')).toBeVisible();
    await expect(page.locator('.ant-switch')).toBeVisible();
  });

  test('切换到子会话标签应加载并展示该子会话消息', async ({ page }) => {
    await setupMocks(page);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    await page.locator('.ant-tabs-tab').nth(1).click();
    await page.waitForTimeout(500);

    await expect(page.locator('text=子会话问题')).toBeVisible();
    await expect(page.locator('text=子会话回答')).toBeVisible();
  });

  test('子会话标签为只读视图：无输入框、模型选择、思考开关及发送/回滚按钮', async ({ page }) => {
    await setupMocks(page);
    await page.goto('/sessions/session-1/chat');
    await page.waitForSelector('.ant-tabs');

    await page.locator('.ant-tabs-tab').nth(1).click();
    await page.waitForTimeout(500);

    await expect(page.getByPlaceholder('输入消息，Enter 发送，Shift+Enter 换行')).not.toBeVisible();
    await expect(page.locator('.ant-select')).not.toBeVisible();
    await expect(page.locator('.ant-switch')).not.toBeVisible();
    await expect(page.getByRole('button', { name: '发送' })).not.toBeVisible();
    await expect(page.getByRole('button', { name: '回滚' })).not.toBeVisible();
  });
});
