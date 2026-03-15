/**
 * 服务保护配置相关 API
 */
import axios from 'axios'
import type { ProtectionConfig, ProtectionConfigForm } from '@/types'

interface ConfigsResponse {
  configs: ProtectionConfig[]
}

export const protectionApi = {
  /**
   * 获取所有保护配置
   */
  list: async (): Promise<ProtectionConfig[]> => {
    const response = await axios.get('/api/v1/protection/configs')
    return response.data.configs || []
  },

  /**
   * 获取单个服务的保护配置
   */
  get: async (serviceName: string): Promise<ProtectionConfig | null> => {
    try {
      const response = await axios.get(`/api/v1/protection/configs/${encodeURIComponent(serviceName)}`)
      return response.data
    } catch {
      return null
    }
  },

  /**
   * 更新熔断器配置
   */
  updateCircuitBreaker: async (serviceName: string, config: {
    enabled: boolean
    threshold: number
    timeout: number
  }): Promise<void> => {
    await axios.put(`/api/v1/protection/configs/${encodeURIComponent(serviceName)}/circuit-breaker`, config)
  },

  /**
   * 更新限流器配置
   */
  updateRateLimiter: async (serviceName: string, config: {
    enabled: boolean
    permits: number
  }): Promise<void> => {
    await axios.put(`/api/v1/protection/configs/${encodeURIComponent(serviceName)}/rate-limiter`, config)
  },

  /**
   * 更新集群配置
   */
  updateCluster: async (serviceName: string, config: {
    timeoutMs: number
    retries: number
    clusterStrategy: string
  }): Promise<void> => {
    await axios.put(`/api/v1/protection/configs/${encodeURIComponent(serviceName)}/cluster`, config)
  },

  /**
   * 保存完整配置（组合上述操作）
   */
  saveConfig: async (form: ProtectionConfigForm): Promise<void> => {
    await protectionApi.updateCircuitBreaker(form.serviceName, {
      enabled: form.circuitBreakerEnabled,
      threshold: form.circuitBreakerThreshold,
      timeout: form.circuitBreakerTimeout
    })

    await protectionApi.updateRateLimiter(form.serviceName, {
      enabled: form.rateLimiterEnabled,
      permits: form.rateLimiterPermits
    })

    await protectionApi.updateCluster(form.serviceName, {
      timeoutMs: form.timeoutMs,
      retries: form.retries,
      clusterStrategy: form.clusterStrategy
    })
  },

  /**
   * 删除保护配置
   */
  delete: async (serviceName: string): Promise<void> => {
    await axios.delete(`/api/v1/protection/configs/${encodeURIComponent(serviceName)}`)
  }
}