import { test, expect, Page } from '@playwright/test';

const EVAL_ID = 'eval-100';
const RESULT_ID_PREFIX = 'result-';

const MOCK_EVALUATION = {
  id: EVAL_ID,
  name: '测试评估',
  description: 'e2e 测试用评估',
  agentEvalId: 'agent-eval-1',
  agentId: 'agent-1',
  executionCount: 1,
  modelId: 'model-1',
  executionType: 'BACKGROUND',
};

const MOCK_RESULTS = [
  { id: `${RESULT_ID_PREFIX}1`, evaluationId: EVAL_ID, evaluationSessionId: 'session-1', result: 'r1', totalTokenUsed: '100', modelId: 'model-1', finalScore: 90, createTime: '2026-07-31T00:00:00' },
  { id: `${RESULT_ID_PREFIX}2`, evaluationId: EVAL_ID, evaluationSessionId: 'session-2', result: 'r2', totalTokenUsed: '200', modelId: 'model-1', finalScore: 75, createTime: '2026-07-31T00:01:00' },
  { id: `${RESULT_ID_PREFIX}3`, evaluationId: EVAL_ID, evaluationSessionId: 'session-3', result: 'r3', totalTokenUsed: '300', modelId: 'model-1', finalScore: 55, createTime: '2026-07-31T00:02:00' },
];

async function setupMocks(page: Page, opts?: { batchSuccess?: boolean; clearSuccess?: boolean }) {
  await page.route(`**/api/evaluations/${EVAL_ID}`, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_EVALUATION }) });
  });
  await page.route(`**/api/evaluations/${EVAL_ID}/results`, async (route) => {
    if (route.request().method() === 'DELETE') {
      const ok = opts?.clearSuccess !== false;
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: ok, message: ok ? '操作成功' : '系统内部错误', data: null }) });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_RESULTS }) });
  });
  await page.route('**/api/models*', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: [] }) });
  });
  await page.route('**/api/evaluations/results/batch-delete', async (route) => {
    const ok = opts?.batchSuccess !== false;
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: ok, message: ok ? '操作成功' : '系统内部错误', data: null }) });
  });
}

async function selectRow(page: Page, index: number) {
  const checkbox = page
    .locator('.ant-table-tbody tr[data-row-key]')
    .nth(index)
    .locator('.ant-checkbox');
  await checkbox.click();
}

test.describe('EvaluationResultList 批量删除与清空', () => {
  test.beforeEach(async ({ page }) => {
    await setupMocks(page);
  });

  test('未勾选任何行时批量删除按钮应 disabled', async ({ page }) => {
    await page.goto(`/evaluations/items/${EVAL_ID}/results`);
    await page.waitForSelector('.ant-table');

    const batchBtn = page.locator('button:has-text("批量删除")');
    await expect(batchBtn).toBeDisabled();
  });

  test('勾选行后批量删除按钮应可点击', async ({ page }) => {
    await page.goto(`/evaluations/items/${EVAL_ID}/results`);
    await page.waitForSelector('.ant-table');

    await selectRow(page, 0);
    const batchBtn = page.locator('button:has-text("批量删除")');
    await expect(batchBtn).toBeEnabled();
  });

  test('点击批量删除应弹出确认框，确认后调用 POST /evaluations/results/batch-delete', async ({ page }) => {
    await page.goto(`/evaluations/items/${EVAL_ID}/results`);
    await page.waitForSelector('.ant-table');

    await selectRow(page, 0);
    await selectRow(page, 1);

    const [request] = await Promise.all([
      page.waitForRequest(
        (req) =>
          req.method() === 'POST' &&
          req.url().includes('/api/evaluations/results/batch-delete'),
      ),
      (async () => {
        await page.locator('button:has-text("批量删除")').click();
        await page.locator('.ant-modal-confirm').waitFor();
        await page.locator('.ant-modal-confirm .ant-btn-dangerous').click();
      })(),
    ]);

    const postData = request.postDataJSON();
    expect(postData).toEqual([`${RESULT_ID_PREFIX}1`, `${RESULT_ID_PREFIX}2`]);
  });

  test('批量删除成功后应提示成功并清空选中', async ({ page }) => {
    await page.goto(`/evaluations/items/${EVAL_ID}/results`);
    await page.waitForSelector('.ant-table');

    await selectRow(page, 0);
    await page.locator('button:has-text("批量删除")').click();
    await page.locator('.ant-modal-confirm .ant-btn-dangerous').click();

    await expect(page.locator('.ant-message-notice-content:has-text("批量删除成功")')).toBeVisible();
    const batchBtn = page.locator('button:has-text("批量删除")');
    await expect(batchBtn).toBeDisabled();
  });

  test('批量删除失败时应提示错误', async ({ page }) => {
    await setupMocks(page, { batchSuccess: false });
    await page.goto(`/evaluations/items/${EVAL_ID}/results`);
    await page.waitForSelector('.ant-table');

    await selectRow(page, 0);
    await page.locator('button:has-text("批量删除")').click();
    await page.locator('.ant-modal-confirm .ant-btn-dangerous').click();

    await expect(page.locator('.ant-message-notice-content:has-text("批量删除失败")')).toBeVisible();
  });

  test('清空按钮应始终可点（未勾选行时也可点击）', async ({ page }) => {
    await page.goto(`/evaluations/items/${EVAL_ID}/results`);
    await page.waitForSelector('.ant-table');

    const clearBtn = page.locator('button', { hasText: /清\s*空/ });
    await expect(clearBtn).toBeEnabled();
  });

  test('点击清空应弹出确认框，确认后调用 DELETE /evaluations/{id}/results', async ({ page }) => {
    await page.goto(`/evaluations/items/${EVAL_ID}/results`);
    await page.waitForSelector('.ant-table');

    const [request] = await Promise.all([
      page.waitForRequest(
        (req) =>
          req.method() === 'DELETE' &&
          req.url().includes(`/api/evaluations/${EVAL_ID}/results`),
      ),
      (async () => {
        await page.locator('button', { hasText: /清\s*空/ }).click();
        await page.locator('.ant-modal-confirm').waitFor();
        await page.locator('.ant-modal-confirm .ant-btn-dangerous').click();
      })(),
    ]);

    expect(request.url()).toContain(`/api/evaluations/${EVAL_ID}/results`);
  });

  test('清空成功后应提示成功并刷新列表', async ({ page }) => {
    await page.goto(`/evaluations/items/${EVAL_ID}/results`);
    await page.waitForSelector('.ant-table');

    await page.locator('button', { hasText: /清\s*空/ }).click();
    await page.locator('.ant-modal-confirm .ant-btn-dangerous').click();

    await expect(page.locator('.ant-message-notice-content:has-text("清空成功")')).toBeVisible();
  });

  test('清空失败时应提示错误', async ({ page }) => {
    await setupMocks(page, { clearSuccess: false });
    await page.goto(`/evaluations/items/${EVAL_ID}/results`);
    await page.waitForSelector('.ant-table');

    await page.locator('button', { hasText: /清\s*空/ }).click();
    await page.locator('.ant-modal-confirm .ant-btn-dangerous').click();

    await expect(page.locator('.ant-message-notice-content:has-text("清空失败")')).toBeVisible();
  });
});
