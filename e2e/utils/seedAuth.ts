import type { Page } from '@playwright/test';

/** 管理员用户 JSON（userType=2），写入 localStorage currentUser。 */
const ADMIN_USER = {
  id: '1',
  loginName: 'admin',
  displayName: '管理员',
  userType: 2,
  enabled: 1,
};

/** 普通用户 JSON（userType=1），写入 localStorage currentUser。 */
const NORMAL_USER = {
  id: '2',
  loginName: 'user',
  displayName: '普通用户',
  userType: 1,
  enabled: 1,
};

/**
 * 在页面脚本执行前注入管理员登录态（localStorage currentUser），
 * 供 e2e 用例绕过登录守卫直接访问主界面页面。
 * @param page Playwright 页面
 */
export async function seedAdminLogin(page: Page): Promise<void> {
  await page.addInitScript((user) => {
    localStorage.setItem('currentUser', JSON.stringify(user));
  }, ADMIN_USER);
}

/**
 * 在页面脚本执行前注入普通用户登录态（localStorage currentUser），
 * 供 e2e 用例绕过登录守卫直接访问主界面页面。
 * @param page Playwright 页面
 */
export async function seedNormalLogin(page: Page): Promise<void> {
  await page.addInitScript((user) => {
    localStorage.setItem('currentUser', JSON.stringify(user));
  }, NORMAL_USER);
}
