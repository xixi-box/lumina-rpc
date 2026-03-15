/**
 * 统计数据相关 API
 */
import axios from 'axios'
import type { StatsData, TrendDataPoint } from '@/types'

export const statsApi = {
  /**
   * 获取大盘统计数据
   */
  get: async (): Promise<StatsData> => {
    const response = await axios.get('/api/v1/stats')
    return response.data || {}
  },

  /**
   * 获取趋势数据
   */
  getTrend: async (minutes: number = 60): Promise<TrendDataPoint[]> => {
    const response = await axios.get(`/api/v1/stats/trend?minutes=${minutes}`)
    return response.data || []
  },

  /**
   * 获取实时统计
   */
  getRealtime: async (): Promise<{
    totalRequests: number
    successCount: number
    failCount: number
    avgLatency: number
    serviceCount: number
  }> => {
    const response = await axios.get('/api/v1/stats/realtime')
    return response.data || {}
  }
}