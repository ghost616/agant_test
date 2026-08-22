import { test, expect, Page, WebSocketRoute } from '@playwright/test';
import { seedAdminLogin } from './utils/seedAuth';

/**
 * 补充 E2E：子→主回传 WebSocket 消息自动切回主会话标签（本次变更核心行为）。
 *
 * 覆盖：
 * 1. 正向：用户停留在子会话标签时，收到子→主回传（SEND_USER_MESSAGE，sessionId === 页面主会话），
 *    页面无条件 setActivePath([sessionId]) 切回主会话标签（主会话回复立即可见）。
 * 2. 防御/反向：用户停留在子会话标签时，收到主→子发送（sessionId 为子会话），
 *    路径按 parentSessionIds 父链展开，不经过 setActivePath([sessionId])（标签不被强制切回主会话）。
 */

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

/** 模拟 /api/chat SSE 响应：立即输出一条增量并 stop 结束。 */
const MOCK_SSE_BODY = [
  'data: {"delta":"主会话已收到子会话回传","finishReason":null}',
  'data: {"finishReason":"stop","hasToolCalls":false}',
].join('\n') + '\n';

async function setupMocks(page: Page) {
  await page.route('**/api/sessions/session-1', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_SESSION }) });
  });
  await page.route('**/api/sessions/session-1/messages', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_MESSAGES }) });
  });
  await page.route('**/api/sessions/session-1/children', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_CHILD_SESSIONS }) });
  });
  await page.route('**/api/sessions/child-1/children', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: [] }) });
  });
  await page.route('**/api/sessions/child-1/messages', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_CHILD_MESSAGES }) });
  });
  await page.route('**/api/models*', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: [] }) });
  });
  // 主会话续接（continueMainChat / runChildSessionFlow 均会 POST /api/chat）：返回模拟 SSE 流
  await page.route('**/api/chat', async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({ status: 200, contentType: 'text/event-stream', body: MOCK_SSE_BODY });
    } else {
      await route.fulfill({ status: 405, contentType: 'application/json', body: JSON.stringify({ message: 'method not allowed' }) });
    }
  });
}

/**
 * 拦截 /ws 并暴露 WebSocketRoute，供测试在指定时机向页面注入服务端推送。
 */
function captureWsRoute(page: Page): () => WebSocketRoute | null {
  let wsRoute: WebSocketRoute | null = null;
  page.routeWebSocket('**/ws', (ws) => {
    wsRoute = ws;
    // 不连接真实服务端：页面侧连接保持打开（模拟模式），仅由测试注入消息
  });
  return () => wsRoute;
}

/** 进入会话页并选中子会话（路径 [主会话][子会话1]）。 */
async function openAndSelectChild(page: Page) {
  await page.goto('/sessions/session-1/chat');
  await page.waitForSelector('.ant-tabs');
  await expect(page.locator('.ant-tabs-tab').first()).toHaveText('主会话');
  // 展开计数标签并选择子会话1
  await page.locator('.agent-chat-count-tab').click();
  await page.locator('.ant-dropdown-menu-item', { hasText: '子会话1' }).click();
  await page.waitForTimeout(500);
  // 路径 [主会话][子会话1]：子会话1 为末位路径项（父层级计数标签隐藏）
  await expect(page.locator('.ant-tabs-tab')).toHaveCount(2);
  await expect(page.locator('.ant-tabs-tab').nth(1)).toHaveText('子会话1');
  await expect(page.locator('.agent-chat-count-tab')).toHaveCount(0);
}

test.beforeEach(async ({ page }) => {
  await seedAdminLogin(page);
});

test.describe('AgentChat WS 子→主回传自动切回主会话标签', () => {
  test('停留在子会话标签时收到子→主回传：无条件切回主会话标签（本次变更核心行为）', async ({ page }) => {
    await setupMocks(page);
    const getWsRoute = captureWsRoute(page);
    await openAndSelectChild(page);

    // 注入子→主回传：SEND_USER_MESSAGE，sessionId === 页面主会话 session-1
    const wsRoute = getWsRoute();
    expect(wsRoute).not.toBeNull();
    wsRoute!.send(
      JSON.stringify({
        messageName: 'SEND_USER_MESSAGE',
        sessionId: 'session-1',
        parentSessionIds: [],
        content: '子会话已完成回传',
      }),
    );

    // 核心断言：onSessionMessage 无条件 setActivePath([sessionId]) →
    // 路径回到 [主会话]，计数标签（子会话 2）恢复显示，主会话视图（输入框）可见
    await expect(page.locator('.ant-tabs-tab')).toHaveCount(2);
    await expect(page.locator('.ant-tabs-tab').first()).toHaveText('主会话');
    await expect(page.locator('.agent-chat-count-tab')).toHaveText('子会话 2');
    await expect(page.getByPlaceholder('输入消息，Enter 发送，Shift+Enter 换行')).toBeVisible();
  });

  test('停留在子会话标签时收到主→子发送：路径按父链展开为 [主会话][子会话1]，不被强制切回主会话（防御分支）', async ({ page }) => {
    await setupMocks(page);
    const getWsRoute = captureWsRoute(page);
    await openAndSelectChild(page);

    // 注入主→子发送：SEND_USER_MESSAGE，sessionId 为子会话 child-1，父链 [session-1]
    const wsRoute = getWsRoute();
    expect(wsRoute).not.toBeNull();
    wsRoute!.send(
      JSON.stringify({
        messageName: 'SEND_USER_MESSAGE',
        sessionId: 'child-1',
        parentSessionIds: ['session-1'],
        content: '子会话回复',
      }),
    );

    // 防御断言：不经过 setActivePath([sessionId])——路径仍为 [主会话][子会话1]
    // （子会话为末位路径项，计数标签仍隐藏），子会话视图仍为只读（无输入框）
    await expect(page.locator('.ant-tabs-tab')).toHaveCount(2);
    await expect(page.locator('.ant-tabs-tab').nth(1)).toHaveText('子会话1');
    await expect(page.locator('.agent-chat-count-tab')).toHaveCount(0);
    await expect(page.getByPlaceholder('输入消息，Enter 发送，Shift+Enter 换行')).not.toBeVisible();
  });
});