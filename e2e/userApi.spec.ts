import { test, expect, APIRequestContext } from '@playwright/test';

/**
 * 用户模块接口测试（真实后端：3000 端口 /api 代理到 8080）。
 *
 * 覆盖：分页仅返回普通用户、创建固定普通用户、修改不再支持用户类型、
 * PUT /api/auth/me 自助修改、POST /api/auth/logout 清除会话与 Cookie、
 * 未登录/无权限/方法/参数等异常路径。
 */

const BASE = 'http://127.0.0.1:3000';

/** 提取 Set-Cookie 中的 SESSION_ID=xxx */
function extractSessionId(setCookie: string | undefined): string | null {
  if (!setCookie) return null;
  const m = setCookie.match(/SESSION_ID=([^;]+)/);
  return m ? m[1] : null;
}

async function login(request: APIRequestContext, loginName: string, password: string) {
  const res = await request.post(`${BASE}/api/auth/login`, {
    data: { loginName, password },
  });
  const body = await res.json();
  return { res, body, sessionId: extractSessionId(res.headers()['set-cookie']) };
}

/** 会话 Cookie 请求头 */
function cookieHeader(sessionId: string): Record<string, string> {
  return { Cookie: `SESSION_ID=${sessionId}` };
}

let adminSessionId: string;
let normalUserId: string;
let normalLoginName: string;
let normalPassword: string;

test.describe.configure({ mode: 'serial' });

test.beforeAll(async ({ request }) => {
  const { body, sessionId } = await login(request, 'admin', '123456');
  expect(body.success).toBe(true);
  expect(body.data.userType).toBe(2);
  expect(sessionId).toBeTruthy();
  adminSessionId = sessionId as string;
});

test('未登录访问 PUT /api/auth/me 抛 USER_NOT_LOGIN', async ({ request }) => {
  const res = await request.put(`${BASE}/api/auth/me`, { data: { displayName: 'x' } });
  const body = await res.json();
  expect(body.code).toBe('USER-NOT-LOGIN');
  expect(body.success).toBe(false);
});

test('未登录访问 POST /api/auth/logout 抛 USER_NOT_LOGIN', async ({ request }) => {
  const res = await request.post(`${BASE}/api/auth/logout`);
  const body = await res.json();
  expect(body.code).toBe('USER-NOT-LOGIN');
});

test('未登录访问 GET /api/users 抛 USER_NOT_LOGIN', async ({ request }) => {
  const res = await request.get(`${BASE}/api/users?page=1&size=10`);
  const body = await res.json();
  expect(body.code).toBe('USER-NOT-LOGIN');
});

test('分页查询仅返回普通用户且 total 不含管理员', async ({ request }) => {
  const api = request;
  const res = await api.get('/api/users?page=1&size=100', { headers: cookieHeader(adminSessionId) });
  const body = await res.json();
  expect(body.success).toBe(true);
  const list = body.data.list as { loginName: string; userType: number }[];
  expect(list.length).toBeGreaterThan(0);
  for (const u of list) {
    expect(u.userType).toBe(1);
    expect(u.loginName).not.toBe('admin');
  }
  expect(body.data.total).toBe(list.length);
});

test('创建用户固定普通用户类型且 enabled 缺省为 1', async ({ request }) => {
  const api = request;
  normalLoginName = `lizhu_api_${Date.now()}`;
  normalPassword = 'Api@123456';
  const res = await api.post('/api/users', {
    headers: cookieHeader(adminSessionId),
    data: { loginName: normalLoginName, displayName: '接口测试用户', password: normalPassword },
  });
  const body = await res.json();
  expect(body.success).toBe(true);
  expect(body.data.userType).toBe(1);
  expect(body.data.enabled).toBe(1);
  expect(body.data.password).toBeUndefined();
  normalUserId = String(body.data.id);
});

test('创建用户 enabled 显式传 0 生效', async ({ request }) => {
  const api = request;
  const name = `lizhu_disabled_${Date.now()}`;
  const res = await api.post('/api/users', {
    headers: cookieHeader(adminSessionId),
    data: { loginName: name, password: 'x123456', enabled: 0 },
  });
  const body = await res.json();
  expect(body.success).toBe(true);
  expect(body.data.enabled).toBe(0);
});

test('创建用户缺少必填字段返回 PARAM_INVALID', async ({ request }) => {
  const api = request;
  const res = await api.post('/api/users', { headers: cookieHeader(adminSessionId), data: { password: 'x123456' } });
  const body = await res.json();
  expect(body.success).toBe(false);
  expect(body.code).toBe('SYS-002');
});

test('修改用户支持 displayName/password/enabled 且不动用户类型', async ({ request }) => {
  const api = request;
  const res = await api.put(`/api/users/${normalUserId}`, {
    headers: cookieHeader(adminSessionId),
    data: { displayName: '接口测试-改名', password: 'New@Pwd2026', enabled: 0 },
  });
  const body = await res.json();
  expect(body.success).toBe(true);
  expect(body.data.displayName).toBe('接口测试-改名');
  expect(body.data.enabled).toBe(0);
  expect(body.data.userType).toBe(1);
  normalPassword = 'New@Pwd2026';
});

test('修改用户请求体携带 userType 被忽略', async ({ request }) => {
  const api = request;
  const res = await api.put(`/api/users/${normalUserId}`, {
    headers: cookieHeader(adminSessionId),
    data: { displayName: '接口测试-最终', userType: 2, enabled: 1 },
  });
  const body = await res.json();
  expect(body.success).toBe(true);
  expect(body.data.userType).toBe(1);
  expect(body.data.enabled).toBe(1);
});

test('新密码可登录而旧密码失败', async ({ request }) => {
  const oldLogin = await login(request, normalLoginName, 'Api@123456');
  expect(oldLogin.body.code).toBe('USER-LOGIN-FAILED');

  const newLogin = await login(request, normalLoginName, normalPassword);
  expect(newLogin.body.success).toBe(true);
  expect(newLogin.body.data.userType).toBe(1);
});

test('普通用户自助修改 PUT /api/auth/me 修改 displayName', async ({ request }) => {
  const { sessionId } = await login(request, normalLoginName, normalPassword);
  expect(sessionId).toBeTruthy();
  const api = request;

  const res = await api.put('/api/auth/me', { headers: cookieHeader(sessionId as string), data: { displayName: '自助昵称' } });
  const body = await res.json();
  expect(body.success).toBe(true);
  expect(body.data.displayName).toBe('自助昵称');
  expect(body.data.userType).toBe(1);
  expect(body.data.password).toBeUndefined();
});

test('自助修改请求体携带 enabled 被忽略（不可自助修改登录开关）', async ({ request }) => {
  const { sessionId } = await login(request, normalLoginName, normalPassword);
  const api = request;

  const res = await api.put('/api/auth/me', { headers: cookieHeader(sessionId as string), data: { displayName: '自助昵称2', enabled: 0 } });
  const body = await res.json();
  expect(body.success).toBe(true);
  expect(body.data.enabled).toBe(1);
  expect(body.data.displayName).toBe('自助昵称2');
});

test('自助修改空字段不修改', async ({ request }) => {
  const { sessionId } = await login(request, normalLoginName, normalPassword);
  const api = request;

  const res = await api.put('/api/auth/me', { headers: cookieHeader(sessionId as string), data: {} });
  const body = await res.json();
  expect(body.success).toBe(true);
  expect(body.data.displayName).toBe('自助昵称2');
});

test('普通用户访问用户管理接口抛 USER_FORBIDDEN', async ({ request }) => {
  const { sessionId } = await login(request, normalLoginName, normalPassword);
  const api = request;

  const res = await api.get('/api/users?page=1&size=10', { headers: cookieHeader(sessionId as string) });
  const body = await res.json();
  expect(body.code).toBe('USER-FORBIDDEN');
});

test('退出登录清除服务端会话并下发 maxAge=0 的 HttpOnly Cookie', async ({ request }) => {
  const { sessionId } = await login(request, normalLoginName, normalPassword);
  const api = request;

  const res = await api.post('/api/auth/logout', { headers: cookieHeader(sessionId as string) });
  const body = await res.json();
  expect(body.success).toBe(true);
  const setCookie = res.headers()['set-cookie'] || '';
  expect(setCookie).toContain('SESSION_ID=');
  expect(setCookie).toContain('Max-Age=0');
  expect(setCookie).toContain('HttpOnly');

  // 服务端会话已清除：同一 Cookie 再访问受保护接口应 USER_NOT_LOGIN
  const after = await api.get('/api/users?page=1&size=10', { headers: cookieHeader(sessionId as string) });
  const afterBody = await after.json();
  expect(afterBody.code).toBe('USER-NOT-LOGIN');
});

test('PUT 接口仅支持 PUT 方法（GET 返回错误而非成功）', async ({ request }) => {
  const { sessionId } = await login(request, normalLoginName, normalPassword);
  const api = request;
  // 注：应用 GlobalExceptionHandler 将方法不支持异常（405）统一转成 200 + SYS-001 错误响应
  const res = await api.get('/api/auth/me', { headers: cookieHeader(sessionId as string) });
  const body = await res.json();
  expect(body.success).toBe(false);
});