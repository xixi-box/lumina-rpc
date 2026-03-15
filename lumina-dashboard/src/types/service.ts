/**
 * 服务实例类型定义
 */
export interface ServiceInstance {
  id: number
  serviceName: string
  instanceId: string
  host: string
  port: number
  status: string
  version?: string
  metadata?: string
  serviceMetadata?: string
  lastHeartbeat?: string
  registeredAt?: string
  expiresAt?: string
}

/**
 * 服务信息（简化版）
 */
export interface ServiceInfo {
  name: string
  metadata?: any
  methodCount?: number
}

/**
 * 方法信息
 */
export interface MethodInfo {
  name: string
  parameterTypes: string[]
  parameters: any[]
  returnType: string
}

/**
 * 服务元数据
 */
export interface ServiceMetadata {
  interfaceName: string
  methods: MethodInfo[]
}