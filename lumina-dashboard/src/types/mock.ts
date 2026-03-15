/**
 * Mock 规则类型定义
 */
export interface MockRule {
  id: number
  serviceName: string
  methodName: string
  mockType: 'SHORT_CIRCUIT' | 'TAMPER'
  responseType: string
  responseBody: string
  enabled: boolean
  conditionType?: string
  conditionExpression?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * Mock 规则表单
 */
export interface MockRuleForm {
  serviceName: string
  methodName: string
  mockType: 'SHORT_CIRCUIT' | 'TAMPER'
  responseType: string
  responseBody: string
  enabled: boolean
  conditionType?: string
  conditionExpression?: string
}