/**
 * 请求记录类型定义
 */
export interface RequestRecord {
  serviceName: string
  methodName: string
  args: any
  response: any
  success: boolean
  duration: number
  timestamp: string
  mocked?: boolean
  cluster?: string
  retries?: number
  traceId?: string
}

/**
 * 统计数据
 */
export interface StatsData {
  onlineServices?: number
  enabledMockRules?: number
  totalInstances?: number
  totalMockRules?: number
  todayRequests?: number
  avgLatency?: number
  systemStatus?: string
  timestamp?: string
}

/**
 * 趋势数据点
 */
export interface TrendDataPoint {
  time: string | Date
  totalRequests: number
  successCount: number
  failCount: number
  totalLatency: number
  avgLatency: number
}