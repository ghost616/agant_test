import { test, expect, Page } from '@playwright/test';

const AGENT_ID = 'agent-100';

const MOCK_MODEL = {
  id: 'model-1',
  name: 'LLM模型A',
  platformType: 'openai',
  modelName: 'gpt-4',
};

const MOCK_VECTOR_MODEL = {
  id: 'vec-model-1',
  name: '向量模型A',
  platformType: 'openai',
  modelName: 'text-embedding-3-small',
};

const MOCK_TOOL = {
  id: 'tool-1',
  name: '工具A',
  toolType: 'function',
};

const MOCK_SKILL = {
  id: 'skill-1',
  name: '技能A',
};

const MOCK_KB_A = {
  id: 'kb-100',
  name: '测试知识库A',
  status: 'ENABLED',
};

const MOCK_KB_B = {
  id: 'kb-101',
  name: '测试知识库B',
  status: 'ENABLED',
};

const MOCK_AGENT_MEMORY_ON = {
  id: AGENT_ID,
  name: '开启记忆智能体',
  description: 'desc',
  status: 'ENABLED',
  tools: [],
  skills: [],
  knowledgeBases: [],
  recentMessageCount: 10,
  memoryEnabled: true,
  memoryGroupCount: 20,
  vectorModelId: 'vec-model-1',
  createTime: '2026-08-01T00:00:00',
  updateTime: '2026-08-01T00:00:00',
};

const MOCK_AGENT_MEMORY_OFF = {
  id: 'agent-101',
  name: '关闭记忆智能体',
  description: '',
  status: 'DISABLED',
  tools: [],
  skills: [],
  knowledgeBases: [],
  recentMessageCount: 5,
  memoryEnabled: false,
  memoryGroupCount: 30,
  vectorModelId: 'vec-model-1',
  createTime: '2026-08-01T00:00:00',
  updateTime: '2026-08-01T00:00:00',
};

let vectorModelListCalls = 0;

async function setupMocks(
  page: Page,
  agents: unknown[] = [MOCK_AGENT_MEMORY_ON, MOCK_AGENT_MEMORY_OFF],
) {
  vectorModelListCalls = 0;
  await page.route('**/api/knowledge-bases*', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [MOCK_KB_A, MOCK_KB_B] }),
      });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
  });
  await page.route('**/api/models*', async (route) => {
    const url = route.request().url();
    const hasEmbeddings = url.includes('modelType=EMBEDDINGS');
    const data = hasEmbeddings ? [MOCK_VECTOR_MODEL] : [MOCK_MODEL];
    if (hasEmbeddings) {
      vectorModelListCalls += 1;
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data }),
    });
  });
  await page.route('**/api/tools*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: [MOCK_TOOL] }),
    });
  });
  await page.route('**/api/skills*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: [MOCK_SKILL] }),
    });
  });
  await page.route('**/api/agents*', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: agents }),
      });
      return;
    }
    if (route.request().method() === 'POST') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: MOCK_AGENT_MEMORY_ON }),
      });
      return;
    }
    if (route.request().method() === 'PUT') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: MOCK_AGENT_MEMORY_ON }),
      });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
  });
}

function memorySwitch(page: Page) {
  return page
    .locator('.ant-modal .ant-form-item')
    .filter({ hasText: '记忆功能' })
    .locator('.ant-switch');
}

function vectorModelFormItem(page: Page) {
  return page
    .locator('.ant-modal .ant-form-item')
    .filter({ hasText: '向量模型' });
}

function vectorModelSelect(page: Page) {
  return vectorModelFormItem(page).locator('.ant-select');
}

test.describe('智能体管理页 - 向量模型', () => {
  test.setTimeout(180000);
  test.beforeEach(async ({ page }) => {
    await setupMocks(page);
    await page.goto('/agents', { waitUntil: 'domcontentloaded', timeout: 120000 });
    await page.waitForSelector('.ant-table', { timeout: 120000 });
  });

  test('加载页面时按 EMBEDDINGS 类型并行请求模型列表', async ({ page }) => {
    await expect.poll(() => vectorModelListCalls).toBeGreaterThan(0);
  });

  test('新增弹窗：记忆关闭时「向量模型」表单项隐藏', async ({ page }) => {
    await page.getByRole('button', { name: '新增智能体' }).click();
    await page.locator('.ant-modal').waitFor();
    await expect(vectorModelFormItem(page)).not.toBeVisible();
  });

  test('新增弹窗：开启记忆后「向量模型」下拉显示，选项为向量模型', async ({ page }) => {
    await page.getByRole('button', { name: '新增智能体' }).click();
    await page.locator('.ant-modal').waitFor();
    await memorySwitch(page).click();

    const select = vectorModelSelect(page);
    await expect(select).toBeVisible();
    await select.click();
    const dropdown = page.locator('.ant-select-dropdown:visible');
    await expect(dropdown.getByText('向量模型A')).toBeVisible();
  });

  test('编辑回填：memoryEnabled=true 时回填 vectorModelId', async ({ page }) => {
    const row = page.getByRole('row', { name: /开启记忆智能体/ });
    await row.getByRole('button', { name: '编辑' }).click();
    await page.locator('.ant-modal').waitFor();

    const item = vectorModelFormItem(page);
    await expect(item).toBeVisible();
    await expect(item).toContainText('向量模型A');
  });

  test('编辑回填：memoryEnabled=false 时「向量模型」隐藏，提交 PUT 时 vectorModelId 置为 undefined', async ({ page }) => {
    const row = page.getByRole('row', { name: /关闭记忆智能体/ });
    await row.getByRole('button', { name: '编辑' }).click();
    await page.locator('.ant-modal').waitFor();
    await expect(vectorModelFormItem(page)).not.toBeVisible();

    const [request] = await Promise.all([
      page.waitForRequest(
        (req) =>
          req.method() === 'PUT' && req.url().includes(`/api/agents/agent-101`),
      ),
      (async () => {
        await page.locator('.ant-modal-footer .ant-btn-primary').click();
      })(),
    ]);
    const payload = request.postDataJSON();
    expect(payload.memoryEnabled).toBe(false);
    expect(payload.vectorModelId).toBeUndefined();
  });

  test('新增提交：开启记忆并选向量模型后 POST payload 携带 vectorModelId', async ({ page }) => {
    await page.getByRole('button', { name: '新增智能体' }).click();
    await page.locator('.ant-modal').waitFor();
    await page.locator('.ant-modal').getByLabel('名称').fill('向量模型智能体');
    await memorySwitch(page).click();

    const select = vectorModelSelect(page);
    await select.click();
    await page.locator('.ant-select-dropdown:visible').getByText('向量模型A').click();

    const [request] = await Promise.all([
      page.waitForRequest(
        (req) => req.method() === 'POST' && req.url().includes('/api/agents'),
      ),
      (async () => {
        await page.locator('.ant-modal-footer .ant-btn-primary').click();
      })(),
    ]);
    const payload = request.postDataJSON();
    expect(payload.memoryEnabled).toBe(true);
    expect(payload.vectorModelId).toBe('vec-model-1');
  });
});
