import { test, expect, Page } from '@playwright/test';

const MODEL_ID = 'model-embed-1';

async function setupMocks(page: Page) {
  await page.route(`**/api/models/${MODEL_ID}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: MODEL_ID,
          name: '嵌入测试模型',
          platformType: 'OPENAI',
          modelType: 'EMBEDDINGS',
          apiKey: '',
          baseUrl: '',
          modelName: 'text-embedding-3',
          temperature: 0.7,
          maxTokens: 2048,
          status: 'ENABLED',
          description: '',
          createTime: '2026-07-31T00:00:00',
          updateTime: '2026-07-31T00:00:00',
        },
      }),
    });
  });
  await page.route(`**/api/models/${MODEL_ID}/embed`, async (route) => {
    const body = route.request().postDataJSON();
    const input = (body?.input ?? '') as string;
    const embedding = Array.from({ length: 150 }, (_, i) => (i + 1) / 100);
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          embeddings: [{ index: 0, embedding }],
          usage: { promptTokens: input.length, completionTokens: 0, totalTokens: input.length },
        },
      }),
    });
  });
}

test.describe('ModelTest EMBEDDINGS 界面', () => {
  test.beforeEach(async ({ page }) => {
    await setupMocks(page);
  });

  test('应渲染嵌入测试界面并展示 modelType 标签', async ({ page }) => {
    await page.goto(`/models/${MODEL_ID}/test`);
    await expect(page.getByText('嵌入测试模型')).toBeVisible();
    await expect(page.getByText('EMBEDDINGS')).toBeVisible();
    await expect(page.getByText('输入文本获取向量表示')).toBeVisible();
    await expect(page.getByPlaceholder('输入文本，最多 1000 字符')).toBeVisible();
  });

  test('发送文本应调用 embed API 并展示向量结果（前 100 维加省略号）', async ({ page }) => {
    await page.goto(`/models/${MODEL_ID}/test`);

    const [request] = await Promise.all([
      page.waitForRequest((req) => req.url().includes(`/api/models/${MODEL_ID}/embed`)),
      page.getByPlaceholder('输入文本，最多 1000 字符').fill('你好世界'),
      page.getByRole('button', { name: /发\s*送/ }).click(),
    ]);
    expect(request.method()).toBe('POST');
    expect(request.postDataJSON()).toMatchObject({ input: '你好世界', model: 'text-embedding-3' });

    await expect(page.getByText(/^\[.*\.\.\.\]$/)).toBeVisible();
  });

  test('输入超过 1000 字符应被截断', async ({ page }) => {
    await page.goto(`/models/${MODEL_ID}/test`);
    const textarea = page.getByPlaceholder('输入文本，最多 1000 字符');
    await textarea.fill('a'.repeat(1005));
    const value = await textarea.inputValue();
    expect(value).toHaveLength(1000);
  });

  test('点击清空应清空输入与结果', async ({ page }) => {
    await page.goto(`/models/${MODEL_ID}/test`);
    const textarea = page.getByPlaceholder('输入文本，最多 1000 字符');
    await textarea.fill('待清空文本');
    await page.getByRole('button', { name: /发\s*送/ }).click();
    await expect(page.getByText(/^\[.*\.\.\.\]$/)).toBeVisible();

    await page.getByRole('button', { name: /清\s*空/ }).click();
    await expect(textarea).toHaveValue('');
    await expect(page.getByText('输入文本获取向量表示')).toBeVisible();
  });
});
