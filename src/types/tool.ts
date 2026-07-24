import type { CommonStatus } from './common';

export type ToolType = 'JAVA' | 'TYPESCRIPT' | 'PYTHON' | 'MCP_HTTP' | 'CUSTOM';

export type SubToolType = 'BROWSER';

export interface ToolConfig {
  id: string;
  name: string;
  toolType: ToolType;
  description: string;
  parameterSchema: string;
  returnSchema: string;
  implPath: string;
  authConfig?: string;
  subToolType?: SubToolType;
  toolScript?: string;
  status: CommonStatus;
  createTime: string;
  updateTime: string;
}

export type ToolFormData = Omit<ToolConfig, 'id' | 'status' | 'createTime' | 'updateTime'>;
