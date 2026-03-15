/**
 * Mock 规则相关 API
 */
import axios from 'axios'
import type { MockRule, MockRuleForm } from '@/types'

export const mockApi = {
  /**
   * 获取所有 Mock 规则
   */
  list: async (): Promise<MockRule[]> => {
    const response = await axios.get('/api/v1/rules')
    return response.data || []
  },

  /**
   * 创建 Mock 规则
   */
  create: async (rule: MockRuleForm): Promise<MockRule> => {
    const response = await axios.post('/api/v1/rules', rule)
    return response.data
  },

  /**
   * 更新 Mock 规则
   */
  update: async (id: number, rule: Partial<MockRuleForm>): Promise<MockRule> => {
    const response = await axios.put(`/api/v1/rules/${id}`, rule)
    return response.data
  },

  /**
   * 删除 Mock 规则
   */
  delete: async (id: number): Promise<void> => {
    await axios.delete(`/api/v1/rules/${id}`)
  },

  /**
   * 切换 Mock 规则状态
   */
  toggle: async (id: number, enabled: boolean): Promise<MockRule> => {
    const response = await axios.patch(`/api/v1/rules/${id}/toggle`, { enabled })
    return response.data
  },

  /**
   * 批量操作
   */
  batchToggle: async (ids: number[], enabled: boolean): Promise<void> => {
    await axios.post('/api/v1/rules/batch/toggle', { ids, enabled })
  },

  batchDelete: async (ids: number[]): Promise<void> => {
    await axios.post('/api/v1/rules/batch/delete', { ids })
  },

  /**
   * 获取服务的 Mock 规则数量统计
   */
  getStats: async (): Promise<Map<string, number>> => {
    const rules = await mockApi.list()
    const counts = new Map<string, number>()
    rules.forEach((rule) => {
      if (rule.enabled) {
        counts.set(rule.serviceName, (counts.get(rule.serviceName) || 0) + 1)
      }
    })
    return counts
  }
}