import { test, expect, Page } from '@playwright/test';
import { seedAdminLogin } from './utils/seedAuth';

const AGENT_EVAL_ID = 'agent-eval-100';

const MOCK_EVALUATIONS = [
  { id: 'eval-1', name: '评估一', agentEvalId: AGENT_EVAL_ID, agentId: 'agent-1', agentName: '智能体A', executionCount: 1, modelId: 'model-1', executionType: 'BACKGROUND', benchmarkSessionId: 'bench-1', createTime: '2026-07-31T00:00:00' },
  { id: 'eval-2', name: '评估二', agentEvalId: AGENT_EVAL_ID, agentId: 'agent-2', agentName: '智能体B', executionCount: 2, modelId: 'model-1', executionType: 'FOREGROUND', benchmarkSessionId: undefined, createTime: '2026-07-31T00:01:00' },
];

async function setupMocks(page: Page, opts?: { clearSuccess?: boolean }) {
  await page.route(`**/api/evaluations**`, async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: MOCK_EVALUATIONS }) });
      return;
    }
    if (route.request().method() === 'DELETE') {
      const url = route.request().url();
      if (/\/api\/evaluations\/[^/]+\/results$/.test(url)) {
        const ok = opts?.clearSuccess !== false;
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: ok, message: ok ? '操作成功' : '系统内部错误', data: null }) });
        return;
      }
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: null }) });
  });
  await page.route('**/api/models*', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: [{ id: 'model-1', name: '测试模型' }] }) });
  });
}

test.beforeEach(async ({ page }) => {
  await seedAdminLogin(page);
});

test.describe('EvaluationList 清空结果', () => {
  test.beforeEach(async ({ page }) => {
    await setupMocks(page);
  });

  test('操作列应渲染清空结果按钮（位于删除按钮之前）', async ({ page }) => {
    await page.goto(`/evaluations/${AGENT_EVAL_ID}/items`);
    await page.waitForSelector('.ant-table');

    const firstRow = page.locator('.ant-table-tbody tr[data-row-key]').first();
    const cells = firstRow.locator('td');
    const actionsCell = cells.last();

    const clearBtn = actionsCell.locator('button', { hasText: /清\s*空结果/ });
    await expect(clearBtn).toBeVisible();

    const deleteBtn = actionsCell.locator('button:has-text("删除")');
    await expect(deleteBtn).toBeVisible();

    const clearBox = await clearBtn.boundingBox();
    const deleteBox = await deleteBtn.boundingBox();
    expect(clearBox!.x).toBeLessThan(deleteBox!.x);
  });

  test('点击清空结果应弹出确认框，确认后调用 DELETE /evaluations/{id}/results', async ({ page }) => {
    await page.goto(`/evaluations/${AGENT_EVAL_ID}/items`);
    await page.waitForSelector('.ant-table');

    const [request] = await Promise.all([
      page.waitForRequest(
        (req) =>
          req.method() === 'DELETE' &&
          /\/api\/evaluations\/eval-1\/results$/.test(req.url()),
      ),
      (async () => {
        await page.locator('.ant-table-tbody tr[data-row-key]').first().locator('button', { hasText: /清\s*空结果/ }).click();
        await page.locator('.ant-modal-confirm').waitFor();
        await expect(page.locator('.ant-modal-confirm .ant-modal-confirm-title')).toHaveText('清空结果');
        await page.locator('.ant-modal-confirm .ant-btn-primary').click();
      })(),
    ]);

    expect(request.url()).toMatch(/\/api\/evaluations\/eval-1\/results$/);
  });

  test('清空成功后应提示成功', async ({ page }) => {
    await page.goto(`/evaluations/${AGENT_EVAL_ID}/items`);
    await page.waitForSelector('.ant-table');

    await page.locator('.ant-table-tbody tr[data-row-key]').first().locator('button', { hasText: /清\s*空结果/ }).click();
    await page.locator('.ant-modal-confirm .ant-btn-primary').click();

    await expect(page.locator('.ant-message-notice-content:has-text("清空成功")')).toBeVisible();
  });

  test('清空失败时应提示错误', async ({ page }) => {
    await setupMocks(page, { clearSuccess: false });
    await page.goto(`/evaluations/${AGENT_EVAL_ID}/items`);
    await page.waitForSelector('.ant-table');

    await page.locator('.ant-table-tbody tr[data-row-key]').first().locator('button', { hasText: /清\s*空结果/ }).click();
    await page.locator('.ant-modal-confirm .ant-btn-primary').click();

    await expect(page.locator('.ant-message-notice-content:has-text("清空失败")')).toBeVisible();
  });
});
