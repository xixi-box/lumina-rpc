/**
 * 服务注册相关 API
 */
import axios from 'axios'
import type { ServiceInstance, ServiceMetadata, ServiceInfo } from '@/types'

export const registryApi = {
  /**
   * 获取所有服务实例
   */
  listInstances: async (): Promise<ServiceInstance[]> => {
    const response = await axios.get('/api/v1/registry/instances')
    return response.data || []
  },

  /**
   * 获取服务元数据
   */
  getMetadata: async (serviceName: string): Promise<ServiceMetadata | null> => {
    const response = await axios.get(`/api/v1/registry/metadata/${serviceName}`)
    const data = response.data
    if (data && data.services && data.services.length > 0) {
      return {
        interfaceName: data.services[0].interfaceName || serviceName,
        methods: data.services[0].methods || []
      }
    }
    return null
  },

  /**
   * 获取服务列表（去重后的服务名）
   */
  listServices: async (): Promise<ServiceInfo[]> => {
    const instances = await registryApi.listInstances()
    const serviceMap = new Map<string, ServiceInfo>()

    for (const instance of instances) {
      if (!serviceMap.has(instance.serviceName)) {
        serviceMap.set(instance.serviceName, {
          name: instance.serviceName,
          metadata: instance.serviceMetadata ? JSON.parse(instance.serviceMetadata) : null
        })
      }
    }

    const services = Array.from(serviceMap.values())

    // 获取每个服务的方法数
    for (const service of services) {
      try {
        const metadata = await registryApi.getMetadata(service.name)
        service.methodCount = metadata?.methods?.length || 0
      } catch {
        service.methodCount = 0
      }
    }

    return services
  },

  /**
   * 注销服务实例
   */
  unregister: async (serviceName: string, instanceId: string): Promise<void> => {
    await axios.delete(`/api/v1/registry/${serviceName}/${instanceId}`)
  }
}