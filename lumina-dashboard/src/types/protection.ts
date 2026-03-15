/**
 * 保护配置类型定义
 */
export interface ProtectionConfig {
  id: number
  serviceName: string
  circuitBreakerEnabled: boolean
  circuitBreakerThreshold: number
  circuitBreakerTimeout: number
  circuitBreakerState?: string
  rateLimiterEnabled: boolean
  rateLimiterPermits: number
  rateLimiterPassed?: number
  rateLimiterRejected?: number
  clusterStrategy: string
  retries: number
  timeoutMs: number
  version: number
}

/**
 * 保护配置表单
 */
export interface ProtectionConfigForm {
  serviceName: string
  circuitBreakerEnabled: boolean
  circuitBreakerThreshold: number
  circuitBreakerTimeout: number
  rateLimiterEnabled: boolean
  rateLimiterPermits: number
  clusterStrategy: string
  retries: number
  timeoutMs: number
}