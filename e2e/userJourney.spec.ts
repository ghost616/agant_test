import { test, expect, Page, APIRequestContext } from '@playwright/test';

/**
 * 用户模块 E2E 用户旅程（真实前后端：前端 3000 静态服务 + /api 代理 8080）。
 *
 * 覆盖：管理员登录落地 /users、用户列表仅含普通用户（无用户类型列）、
 * 添加/修改/禁止登录用户、普通用户登录落地 /models 且无用户管理菜单。
 */

const BASE = 'http://127.0.0.1:3000';

const SUFFIX = Date.now().toString().slice(-8);
const normalName = `lizhu_e2e_${SUFFIX}`;
const normalPwd = 'E2e@123456';

function extractSessionId(setCookie: string | undefined): string | null {
  if (!setCookie) return null;
  const m = setCookie.match(/SESSION_ID=([^;]+)/);
  return m ? m[1] : null;
}

async function apiLogin(request: APIRequestContext, loginName: string, password: string) {
  const res = await request.post(`${BASE}/api/auth/login`, { data: { loginName, password } });
  const body = await res.json();
  return { body, sessionId: extractSessionId(res.headers()['set-cookie']) };
}

/** 通过 UI 登录管理员并等待落地 /users */
async function adminLogin(page: Page): Promise<void> {
  await page.goto('/login');
  await page.getByPlaceholder('请输入登录名').fill('admin');
  await page.getByPlaceholder('请输入密码').fill('123456');
  await page.getByRole('button', { name: /登\s*录/ }).click();
  await expect(page).toHaveURL(/\/users$/);
}

test.describe.configure({ mode: 'serial' });

test.describe('用户管理用户旅程（真实后端）', () => {
  test.beforeAll(async ({ request }) => {
    // 管理员创建普通用户供旅程使用
    const admin = await apiLogin(request, 'admin', '123456');
    expect(admin.body.success).toBe(true);
    const create = await request.post(`${BASE}/api/users`, {
      headers: { Cookie: `SESSION_ID=${admin.sessionId}` },
      data: { loginName: normalName, displayName: '旅程普通用户', password: normalPwd },
    });
    const createBody = await create.json();
    expect(createBody.success).toBe(true);
  });

  test('未登录访问 /users 自动跳转登录页', async ({ page }) => {
    await page.goto('/users');
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByPlaceholder('请输入登录名')).toBeVisible();
  });

  test('管理员登录后落地 /users 且用户管理菜单可见', async ({ page }) => {
    await adminLogin(page);
    await expect(page.locator('.ant-menu').getByText('用户管理')).toBeVisible();
  });

  test('用户列表仅含普通用户且无用户类型列', async ({ page }) => {
    await adminLogin(page);
    await expect(page.getByText('用户管理', { exact: true }).first()).toBeVisible();
    // 表格出现：包含管理员创建的普通用户
    const row = page.locator('tr', { hasText: normalName });
    await expect(row.first()).toBeVisible({ timeout: 15000 });
    // 管理员（admin，userType=2）不应出现在列表中
    await expect(page.locator('tr').filter({ hasText: 'admin' }).filter({ hasText: '管理员' })).toHaveCount(0);
    // 不渲染用户类型列
    const headers = await page.locator('.ant-table-thead th').allTextContents();
    expect(headers.join(',')).not.toContain('用户类型');
  });

  test('添加用户：固定为普通用户并默认允许登录', async ({ page }) => {
    await adminLogin(page);
    const addName = `lizhu_add_${SUFFIX}`;
    await page.getByRole('button', { name: '添加用户' }).click();
    await page.getByPlaceholder('请输入登录名').fill(addName);
    await page.getByPlaceholder('请输入密码').fill('Add@123456');
    // antd 默认 locale 下 Modal 确认按钮为 OK
    await page.getByRole('button', { name: 'OK' }).click();
    await expect(page.getByText('添加成功')).toBeVisible({ timeout: 15000 });
    const row = page.locator('tr', { hasText: addName });
    await expect(row.first()).toBeVisible();
    await expect(row.first()).toContainText('允许登录');
    // 新增用户固定为普通用户：再次登录验证 userType=1
    const resp = await page.request.post(`${BASE}/api/auth/login`, {
      data: { loginName: addName, password: 'Add@123456' },
    });
    const body = await resp.json();
    expect(body.data.userType).toBe(1);
  });

  test('修改用户显示名', async ({ page }) => {
    await adminLogin(page);
    const row = page.locator('tr', { hasText: normalName });
    await expect(row.first()).toBeVisible({ timeout: 15000 });
    await row.first().getByRole('button', { name: '修改' }).click();
    const modal = page.locator('.ant-modal');
    await expect(modal).toBeVisible();
    const displayInput = modal.getByPlaceholder('请输入显示名');
    await displayInput.fill('旅程-已改名');
    await modal.getByRole('button', { name: 'OK' }).click();
    await expect(page.getByText('修改成功')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('tr', { hasText: '旅程-已改名' }).first()).toBeVisible();
  });

  test('禁止/恢复登录开关', async ({ page }) => {
    await adminLogin(page);
    const row = page.locator('tr', { hasText: normalName });
    await expect(row.first()).toBeVisible({ timeout: 15000 });
    await row.first().getByRole('button', { name: '禁止登录' }).click();
    await page.getByRole('button', { name: 'OK' }).click();
    await expect(page.getByText('已禁止登录')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('tr', { hasText: normalName }).first()).toContainText('禁止登录');

    await page.locator('tr', { hasText: normalName }).first().getByRole('button', { name: '恢复登录' }).click();
    await page.getByRole('button', { name: 'OK' }).click();
    await expect(page.getByText('已恢复登录')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('tr', { hasText: normalName }).first()).toContainText('允许登录');
  });

  test('普通用户登录落地 /models 且无用户管理菜单', async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('请输入登录名').fill(normalName);
    await page.getByPlaceholder('请输入密码').fill(normalPwd);
    await page.getByRole('button', { name: /登\s*录/ }).click();
    await expect(page).toHaveURL(/\/models$/);
    await expect(page.locator('.ant-menu').getByText('模型管理')).toBeVisible();
    await expect(page.locator('.ant-menu').getByText('用户管理')).toHaveCount(0);
  });
});