/**
 * 消费者操作台相关 API
 */
import axios from 'axios'

interface ProxyInvokeRequest {
  serviceName: string
  methodName: string
  args: any[]
}

interface ProxyInvokeResponse {
  success: boolean
  data?: any
  error?: string
  message?: string
  mocked?: boolean
  traceId?: string
}

export const consumerApi = {
  /**
   * 代理调用 RPC 服务
   */
  invoke: async (request: ProxyInvokeRequest): Promise<ProxyInvokeResponse> => {
    const response = await axios.post('/api/command/proxy-invoke', request)
    return response.data
  }
}