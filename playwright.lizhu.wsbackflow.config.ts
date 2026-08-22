import { defineConfig } from '@playwright/test';

/**
 * 离朱补充 E2E（AgentChat WS 子→主回传自动切回主会话标签）专用配置：
 * 复用已在运行的静态前端（http://127.0.0.1:3000，/ws、/api 代理到 8080），不启动额外 webServer。
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60000,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://127.0.0.1:3000',
    headless: true,
  },
  testMatch: /AgentChatWsBackflow\.spec\.ts/,
});