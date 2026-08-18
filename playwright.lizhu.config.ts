import { defineConfig } from '@playwright/test';

/**
 * 离朱用户模块测试专用配置：
 * 复用已在运行的静态前端（http://127.0.0.1:3000，/api 代理到 8080）
 * 与后端（http://127.0.0.1:8080），不启动额外 webServer。
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 30000,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://127.0.0.1:3000',
    headless: true,
  },
  testMatch: /userApi\.spec\.ts|userJourney\.spec\.ts/,
});