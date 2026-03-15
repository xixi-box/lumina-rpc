/**
 * 链路追踪相关 API
 */
import axios from 'axios'
import type { TraceDetail, TraceSummary, ServiceStats } from '@/types'

interface TracesResponse {
  success: boolean
  traces: TraceSummary[]
  error?: string
}

export const traceApi = {
  /**
   * 获取最近的 Trace 列表
   */
  list: async (limit: number = 100): Promise<TraceSummary[]> => {
    const response = await axios.get(`/api/v1/traces?limit=${limit}`)
    // 处理可能的响应格式
    if (response.data && Array.isArray(response.data)) {
      return response.data
    } else if (response.data && response.data.traces) {
      return response.data.traces
    }
    return []
  },

  /**
   * 获取 Trace 详情
   */
  getDetail: async (traceId: string): Promise<TraceDetail | null> => {
    const response = await axios.get(`/api/v1/traces/${traceId}`)
    return response.data
  },

  /**
   * 获取服务统计
   */
  getServiceStats: async (hours: number = 1): Promise<ServiceStats[]> => {
    const response = await axios.get(`/api/v1/traces/stats/services?hours=${hours}`)
    return response.data || []
  }
}