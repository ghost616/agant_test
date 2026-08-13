/** 记忆聚合类型，code 对应后端 AggregationType 枚举 */
export type MemoryAggregationType = 'GROUP' | 'DAILY';

/** 会话记忆文档，对应后端 SessionMemoryDocument */
export interface SessionMemoryDocument {
  sessionId: string;
  aggregationType: MemoryAggregationType;
  aggregationStartSeq?: number;
  aggregationEndSeq?: number;
  aggregationStartTime?: number;
  aggregationEndTime?: number;
  aggregationText?: string;
  vector?: number[];
}

/** 记忆聚合查询参数 */
export interface MemoryQueryParams {
  type: MemoryAggregationType;
  page: number;
  size: number;
}

/** 记忆提示语保存请求，对应后端 MemoryPromptSaveRequest */
export interface MemoryPromptSaveRequest {
  prompt: string;
}

/** 聚合文本保存（重新向量化 + 更新 ES）请求，对应后端 MemoryUpdateRequest */
export interface MemoryUpdateRequest {
  /** 目标 ES 文档 ID（sessionId_aggregationType_startSeq_endSeq） */
  docId: string;
  /** 新的聚合摘要文本 */
  text: string;
}

/** 聚合文本重生成请求，对应后端 MemoryRegenerateRequest */
export interface MemoryRegenerateRequest {
  /** 目标 ES 文档 ID（sessionId_aggregationType_startSeq_endSeq） */
  docId: string;
  /** 起始消息序号（含） */
  startSeq?: number;
  /** 结束消息序号（含） */
  endSeq?: number;
  /** 自定义提示语，为空时使用默认聚合提示语 */
  prompt?: string;
}

/** 聚合文本重生成执行状态：RUNNING / COMPLETED / FAILED */
export type MemoryRegenerateStatusEnum = 'RUNNING' | 'COMPLETED' | 'FAILED';

/** 聚合文本重生成状态，对应后端 MemoryRegenerateStatusDTO */
export interface MemoryRegenerateStatus {
  sessionId: string;
  docId?: string;
  /** 执行状态：RUNNING / COMPLETED / FAILED */
  status: MemoryRegenerateStatusEnum;
  /** 重生成得到的聚合摘要文本（COMPLETED 时非空） */
  aggregationText?: string;
  /** 失败原因（FAILED 时非空） */
  error?: string;
}
