import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { ModelConfig } from '../../../types/model';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ id: 'test-id' }),
  };
});

vi.mock('../../../services/model', () => ({
  getModel: vi.fn().mockResolvedValue({
    id: 'test-id',
    name: 'Test Model',
    platformType: 'OPENAI',
    apiKey: '',
    baseUrl: '',
    modelName: 'gpt-4',
    temperature: 0.7,
    maxTokens: 2048,
    status: 'ENABLED',
    description: '',
    createTime: '',
    updateTime: '',
  } satisfies ModelConfig),
  chatStream: vi.fn(),
}));

function renderComponent() {
  return render(
    <MemoryRouter>
      <ModelTest />
    </MemoryRouter>,
  );
}

import ModelTest from '../ModelTest';

describe('ModelTest 返回按钮', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it('应渲染返回按钮', async () => {
    renderComponent();
    const backButton = await screen.findByText('返回');
    expect(backButton).toBeTruthy();
  });

  it('点击返回按钮应导航到 /models', async () => {
    renderComponent();
    const backButton = await screen.findByText('返回');
    fireEvent.click(backButton);
    expect(mockNavigate).toHaveBeenCalledWith('/models');
  });

  it('应显示模型名称和平台类型', async () => {
    renderComponent();
    expect(await screen.findByText('Test Model')).toBeTruthy();
    expect(await screen.findByText('OPENAI')).toBeTruthy();
  });
});
