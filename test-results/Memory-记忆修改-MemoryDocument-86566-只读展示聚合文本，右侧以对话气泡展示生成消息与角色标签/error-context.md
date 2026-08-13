# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: Memory.spec.ts >> 记忆修改 MemoryDocumentDetail 页面 >> 左侧只读展示聚合文本，右侧以对话气泡展示生成消息与角色标签
- Location: e2e\Memory.spec.ts:293:3

# Error details

```
Test timeout of 120000ms exceeded.
```

```
Error: page.goto: Test timeout of 120000ms exceeded.
Call log:
  - navigating to "http://127.0.0.1:5173/memory/1/DAILY", waiting until "load"

```

# Test source

```ts
  194 |       .locator('.ant-table-tbody')
  195 |       .getByRole('button', { name: '按日聚合' })
  196 |       .click();
  197 | 
  198 |     await page.waitForURL('**/memory/1/DAILY');
  199 |     await expect(page.getByText('按日聚合记忆')).toBeVisible();
  200 | 
  201 |     const headerTexts = await page.locator('.ant-table-thead th').allTextContents();
  202 |     expect(headerTexts.some((t) => t.includes('聚合日期'))).toBe(true);
  203 |     await expect(page.locator('.ant-table-tbody').getByText('每日摘要')).toBeVisible();
  204 |   });
  205 | 
  206 |   test('点击按分类聚合跳转 /memory/1/GROUP 并展示起始-结束列', async ({ page }) => {
  207 |     await page.goto('/memory');
  208 |     await page.waitForSelector('.ant-table');
  209 | 
  210 |     await page
  211 |       .locator('.ant-table-tbody')
  212 |       .getByRole('button', { name: '按分类聚合' })
  213 |       .click();
  214 | 
  215 |     await page.waitForURL('**/memory/1/GROUP');
  216 |     await expect(page.getByText('按分类聚合记忆')).toBeVisible();
  217 | 
  218 |     const headerTexts = await page.locator('.ant-table-thead th').allTextContents();
  219 |     expect(headerTexts.some((t) => t.includes('起始-结束'))).toBe(true);
  220 |     await expect(page.locator('.ant-table-tbody').getByText('1 - 3')).toBeVisible();
  221 |     await expect(page.locator('.ant-table-tbody').getByText('分组摘要')).toBeVisible();
  222 |   });
  223 | });
  224 | 
  225 | test.describe('记忆修改 MemoryDetail 页面', () => {
  226 |   test.describe.configure({ timeout: 120000 });
  227 |   test.beforeEach(async ({ page }) => {
  228 |     await setupMocks(page);
  229 |   });
  230 | 
  231 |   test('分页显示共 N 条，切换页码触发 page=2 请求', async ({ page }) => {
  232 |     await page.goto('/memory/1/DAILY');
  233 |     await page.waitForSelector('.ant-table');
  234 | 
  235 |     await expect(page.locator('.ant-pagination-total-text')).toHaveText('共 100 条');
  236 | 
  237 |     const [request] = await Promise.all([
  238 |       page.waitForRequest(
  239 |         (req) =>
  240 |           req.url().includes('/api/sessions/1/memory') && req.url().includes('page=2'),
  241 |       ),
  242 |       page.locator('.ant-pagination-item-2').click(),
  243 |     ]);
  244 |     expect(request.url()).toContain('type=DAILY');
  245 |   });
  246 | 
  247 |   test('返回按钮跳转 /memory', async ({ page }) => {
  248 |     await page.goto('/memory/1/DAILY');
  249 |     await page.waitForSelector('.ant-table');
  250 | 
  251 |     await page.getByRole('button', { name: '返回' }).click();
  252 |     await page.waitForURL('**/memory');
  253 |   });
  254 | 
  255 |   test('聚合列表详情按钮跳转详情页并携带 startSeq/endSeq 参数', async ({ page }) => {
  256 |     await page.goto('/memory/1/DAILY');
  257 |     await page.waitForSelector('.ant-table');
  258 | 
  259 |     const [request] = await Promise.all([
  260 |       page.waitForRequest(
  261 |         (req) =>
  262 |           req.url().includes('/api/sessions/1/messages/range') &&
  263 |           req.url().includes('startSeq=1') &&
  264 |           req.url().includes('endSeq=5'),
  265 |       ),
  266 |       page.locator('.ant-table-tbody').getByRole('button', { name: '详情' }).click(),
  267 |     ]);
  268 | 
  269 |     await page.waitForURL('**/memory/1/DAILY/1-5');
  270 |     expect(request.url()).toContain('/api/sessions/1/messages/range');
  271 |   });
  272 | 
  273 |   test('GROUP 详情按钮跳转 /memory/1/GROUP/1-3', async ({ page }) => {
  274 |     await page.goto('/memory/1/GROUP');
  275 |     await page.waitForSelector('.ant-table');
  276 | 
  277 |     await page
  278 |       .locator('.ant-table-tbody')
  279 |       .getByRole('button', { name: '详情' })
  280 |       .click();
  281 | 
  282 |     await page.waitForURL('**/memory/1/GROUP/1-3');
  283 |     await expect(page.getByText('聚合文档详情')).toBeVisible();
  284 |   });
  285 | });
  286 | 
  287 | test.describe('记忆修改 MemoryDocumentDetail 页面', () => {
  288 |   test.describe.configure({ timeout: 120000 });
  289 |   test.beforeEach(async ({ page }) => {
  290 |     await setupMocks(page);
  291 |   });
  292 | 
  293 |   test('左侧只读展示聚合文本，右侧以对话气泡展示生成消息与角色标签', async ({ page }) => {
> 294 |     await page.goto('/memory/1/DAILY');
      |                ^ Error: page.goto: Test timeout of 120000ms exceeded.
  295 |     await page.waitForSelector('.ant-table');
  296 | 
  297 |     await page
  298 |       .locator('.ant-table-tbody')
  299 |       .getByRole('button', { name: '详情' })
  300 |       .click();
  301 |     await page.waitForURL('**/memory/1/DAILY/1-5');
  302 | 
  303 |     const textarea = page.locator('textarea').first();
  304 |     await expect(textarea).toBeVisible();
  305 |     await expect(textarea).toHaveValue('每日摘要');
  306 |     await expect(textarea).toHaveAttribute('readonly', '');
  307 | 
  308 |     await expect(page.locator('.agent-chat-markdown').getByText('hi')).toBeVisible();
  309 |     await expect(page.locator('.agent-chat-markdown').getByText('hello')).toBeVisible();
  310 |     await expect(page.getByText('你')).toBeVisible();
  311 |     await expect(page.getByText('助手')).toBeVisible();
  312 |   });
  313 | 
  314 |   test('返回按钮跳转 /memory/:sessionId/:type', async ({ page }) => {
  315 |     await page.goto('/memory/1/DAILY');
  316 |     await page.waitForSelector('.ant-table');
  317 | 
  318 |     await page
  319 |       .locator('.ant-table-tbody')
  320 |       .getByRole('button', { name: '详情' })
  321 |       .click();
  322 |     await page.waitForURL('**/memory/1/DAILY/1-5');
  323 | 
  324 |     await page.getByRole('button', { name: '返回' }).click();
  325 |     await page.waitForURL('**/memory/1/DAILY');
  326 |   });
  327 | });
  328 | 
```