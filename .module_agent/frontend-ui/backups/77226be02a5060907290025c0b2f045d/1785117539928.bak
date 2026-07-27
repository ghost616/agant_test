import type { ApiResponse, CommonStatus } from '../types/common';
import type { ChatChunk, ChatRequest, ModelConfig, ModelFormData, PlatformConfig } from '../types/model';
import api from './api';

export interface ModelListParams {
  name?: string;
  platformType?: string;
  status?: string;
}

export async function listModels(
  params: ModelListParams,
): Promise<ModelConfig[]> {
  const res = await api.get<ApiResponse<ModelConfig[]>>('/models', { params });
  return res.data.data;
}

export async function getModel(id: string): Promise<ModelConfig> {
  const res = await api.get<ApiResponse<ModelConfig>>(`/models/${id}`);
  return res.data.data;
}

export async function createModel(data: ModelFormData): Promise<ModelConfig> {
  const res = await api.post<ApiResponse<ModelConfig>>('/models', data);
  return res.data.data;
}

export async function updateModel(
  id: string,
  data: Partial<ModelFormData>,
): Promise<ModelConfig> {
  const res = await api.put<ApiResponse<ModelConfig>>(`/models/${id}`, data);
  return res.data.data;
}

export async function deleteModel(id: string): Promise<void> {
  await api.delete(`/models/${id}`);
}

export async function updateModelStatus(
  id: string,
  status: CommonStatus,
): Promise<void> {
  await api.patch(`/models/${id}/status`, { status });
}

export async function getPlatformConfig(): Promise<PlatformConfig[]> {
  const res = await api.get<ApiResponse<PlatformConfig[]>>('/models/platform-config');
  return res.data.data;
}

export function chatStream(
  id: string,
  request: ChatRequest,
  callbacks: {
    onChunk: (text: string) => void;
    onReasoning: (text: string) => void;
    onDone: () => void;
    onError: (err: Error) => void;
  },
): AbortController {
  const controller = new AbortController();

  const run = async (): Promise<void> => {
    try {
      const response = await fetch(`/api/models/${id}/chat/stream`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
        signal: controller.signal,
      });

      if (!response.ok) {
        const errorText = await response.text().catch(() => '');
        let errorMsg = `请求失败 (${response.status})`;
        try {
          const parsed = JSON.parse(errorText);
          errorMsg = parsed.message || errorMsg;
        } catch {
          // ignore parse error
        }
        throw new Error(errorMsg);
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('无法获取响应流');
      }

      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) {
          callbacks.onDone();
          return;
        }

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (!line.trim()) continue;
          console.log('[chatStream] raw line:', line);
          const jsonStr = line.startsWith('data:') ? line.slice(5) : line;
          try {
            const chunk: ChatChunk = JSON.parse(jsonStr);
            console.log('[chatStream] parsed chunk:', chunk);
            if (chunk.finishReason === 'error') {
              callbacks.onError(new Error(chunk.delta || '模型请求失败'));
              continue;
            }
            if (chunk.finishReason === 'stop') {
              callbacks.onDone();
              return;
            }
            if (chunk.reasoning) {
              callbacks.onReasoning(chunk.reasoning);
            }
            if (chunk.delta) {
              callbacks.onChunk(chunk.delta);
            }
          } catch (parseErr) {
            console.log('[chatStream] JSON parse failed for line:', line, parseErr);
          }
        }
      }
    } catch (err) {
      if (err instanceof DOMException && err.name === 'AbortError') {
        callbacks.onDone();
        return;
      }
      callbacks.onError(err instanceof Error ? err : new Error(String(err)));
    }
  };

  run();
  return controller;
}
