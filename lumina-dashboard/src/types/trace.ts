/**
 * 链路追踪类型定义
 */

/**
 * Span 实体
 */
export interface SpanEntity {
  id: number
  traceId: string
  spanId: string
  parentSpanId: string | null
  serviceName: string
  methodName: string
  kind: 'CLIENT' | 'SERVER'
  startTime: number
  duration: number
  success: boolean
  errorMessage: string | null
  remoteAddress: string | null
  createdAt?: string
}

/**
 * Trace 详情
 */
export interface TraceDetail {
  traceId: string
  spans: SpanEntity[]
  totalDuration: number
  spanCount: number
  hasError: boolean
}

/**
 * Trace 摘要
 */
export interface TraceSummary {
  traceId: string
  serviceName: string
  startTime: number
  spanCount: number
  totalDuration: number
  hasError: boolean
}

/**
 * 服务统计
 */
export interface ServiceStats {
  serviceName: string
  callCount: number
  successCount: number
  errorCount: number
  avgDuration: number
  maxDuration: number
  minDuration: number
}