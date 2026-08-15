import { test, expect, Page } from '@playwright/test';

const ADMIN_USER = {
  id: '1',
  loginName: 'admin',
  displayName: '管理员',
  userType: 2,
  enabled: 1,
  createTime: '2026-08-15T00:00:00',
};

const NORMAL_USER = {
  id: '2',
  loginName: 'user',
  displayName: '普通用户',
  userType: 1,
  enabled: 1,
  createTime: '2026-08-15T00:00:00',
};

async function setupLoginMocks(page: Page, user: unknown) {
  await page.route('**/api/auth/login', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, code: 'SYS-000', message: '操作成功', data: user }),
    });
  });
  await page.route('**/api/users**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, code: 'SYS-000', message: '操作成功', data: { list: [], total: 0, page: 1, size: 10 } }),
    });
  });
  await page.route('**/api/models**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, code: 'SYS-000', message: '操作成功', data: [] }),
    });
  });
}

async function submitLogin(page: Page, loginName: string, password: string) {
  await page.getByPlaceholder('请输入登录名').fill(loginName);
  await page.getByPlaceholder('请输入密码').fill(password);
  await page.getByRole('button', { name: /登\s*录/ }).click();
}

test.describe('登录守卫与角色落地页', () => {
  test('未登录访问任意页面（除 /login）自动跳转登录页', async ({ page }) => {
    await page.goto('/models');
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByPlaceholder('请输入登录名')).toBeVisible();
    await expect(page.getByRole('button', { name: /登\s*录/ })).toBeVisible();
  });

  test('管理员（userType=2）登录后跳转 /users 且用户管理菜单可见', async ({ page }) => {
    await setupLoginMocks(page, ADMIN_USER);
    await page.goto('/');
    await expect(page).toHaveURL(/\/login$/);
    await submitLogin(page, 'admin', '123456');
    await expect(page).toHaveURL(/\/users$/);
    await expect(page.locator('.ant-menu').getByText('用户管理')).toBeVisible();
    await expect(page.locator('.ant-menu').getByText('模型管理')).toBeVisible();
  });

  test('普通用户（userType=1）登录后跳转 /models 且用户管理菜单不可见', async ({ page }) => {
    await setupLoginMocks(page, NORMAL_USER);
    await page.goto('/');
    await expect(page).toHaveURL(/\/login$/);
    await submitLogin(page, 'user', '123456');
    await expect(page).toHaveURL(/\/models$/);
    await expect(page.locator('.ant-menu').getByText('模型管理')).toBeVisible();
    await expect(page.locator('.ant-menu').getByText('用户管理')).toHaveCount(0);
  });
});
