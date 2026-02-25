# Lumina Control Plane

Lumina RPC 的控制平面，负责管理 Mock Rule 并通过 SSE (Server-Sent Events) 实时广播变更通知。

## 核心功能

1. **Mock Rule 管理** - 完整的 CRUD 接口
2. **SSE 长连接** - 实时推送规则变更给 Consumer
3. **心跳保活** - 30秒间隔心跳保持连接
4. **多服务支持** - 按 serviceName 分组广播

## 技术栈

- Spring Boot 3.x
- Spring Data JPA
- H2 Database
- SSE (Server-Sent Events)

## 快速开始

### 编译运行

```bash
# 编译
mvn clean package -pl lumina-control-plane -am -DskipTests

# 运行
java -jar lumina-control-plane/target/lumina-control-plane-1.0-SNAPSHOT.jar
```

### 访问接口

启动后访问：

- API 文档: http://localhost:8080/api/v1/rules
- SSE 订阅: http://localhost:8080/api/v1/sse/subscribe/{serviceName}
- H2 控制台: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/lumina-control-plane`
  - User: `sa`
  - Password: (空)

## API 接口

### Mock Rule 管理

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/v1/rules | 获取所有规则 |
| GET | /api/v1/rules/{id} | 根据 ID 获取规则 |
| GET | /api/v1/rules/service/{serviceName} | 获取指定服务的规则 |
| POST | /api/v1/rules | 创建规则 |
| PUT | /api/v1/rules/{id} | 更新规则 |
| DELETE | /api/v1/rules/{id} | 删除规则 |
| POST | /api/v1/rules/{id}/toggle | 切换规则启用状态 |

### SSE 订阅

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/v1/sse/subscribe/{serviceName} | 订阅指定服务的变更通知 |
| GET | /api/v1/sse/subscribe/all | 订阅所有服务的变更通知 |
| GET | /api/v1/sse/stats | 获取 SSE 连接统计 |

### 健康检查

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/v1/health | 健康状态 |
| GET | /api/v1/health/ready | 就绪状态 |
| GET | /api/v1/health/live | 存活状态 |

## SSE 事件格式

### 连接成功事件

```
event: connected
data: {"status":"connected","service":"order-service"}
```

### 规则变更事件

```
event: rule-change
 id: 123
data: {"ruleId":123,"action":"UPDATE","serviceName":"order-service","timestamp":1698765432100}
```

### 心跳事件

```
event: heartbeat
data: {"type":"heartbeat","timestamp":1698765432100}
```

## 核心实现说明

### 1. SseBroadcastService (核心)

位于 `service/SseBroadcastService.java`，是整个系统最核心的组件：

- 管理所有 SseEmitter 连接
- 按 serviceName 分组存储连接
- 30秒定时心跳保活
- 当 Mock Rule 变化时广播通知

### 2. MockRuleService

位于 `service/MockRuleService.java`：

- 处理规则的 CRUD 操作
- 在规则变更后调用 SseBroadcastService 广播通知
- 支持批量操作

### 3. SseController

位于 `controller/SseController.java`：

- 提供 SSE 订阅端点
- Consumer 通过 `/api/v1/sse/subscribe/{serviceName}` 建立长连接
- 支持查询连接统计

## 注意事项

1. SSE 连接数限制：默认无限制，但受限于服务器资源
2. 心跳间隔：30秒，可根据网络环境调整
3. 连接超时：30分钟，超时后客户端需重新连接
4. 生产环境建议：使用 Nginx 等反向代理时，需配置 SSE 支持
