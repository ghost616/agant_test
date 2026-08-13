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
