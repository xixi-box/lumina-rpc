# Phase 2: 构建 `lumina-control-plane` 与核心 SDK 联动

## 1. 目标
手搓一个轻量级的“Nacos”，实现微服务注册、心跳剔除和 Mock 规则的实时下发。

## 2. 技术栈
Spring Boot 3, Spring Data JPA, MySQL, SseEmitter (Server-Sent Events)。

## 3. 控制面 (Control Plane) 功能要求
- **服务注册与心跳：** 暴露 `POST /api/registry/register` 和 `POST /api/registry/heartbeat`。
- **健康检查：** 启动一个后台定时任务，每隔 10 秒清理一次超过 30 秒未发送心跳的下线实例。
- **长链接推送 (核心亮点)：** 暴露 `GET /api/registry/subscribe/{serviceName}`。Consumer 启动时连上此接口，保持 SSE 长链接。当有管理员在数据库修改了 Mock 规则，或该服务的 Provider 列表发生变化时，立刻通过 SSE push 给 Consumer。

## 4. 回头修改 `lumina-rpc-core`
- 让 Provider 启动时，向 Control Plane 发送注册和定时心跳。
- 让 Consumer 启动时，开启一个后台线程连接 Control Plane 的 SSE 接口。
- **Mock 拦截器逻辑：** 在 Consumer 的 ByteBuddy 代理逻辑中，**在发起真实 Netty 请求之前**，先检查本地内存中是否收到了该方法的 Mock 规则。如果命中，直接反序列化 JSON 并返回，短路掉网络请求！

## 5. 输出动作
完成 `lumina-control-plane` 工程，连接之前指定的 Zeabur MySQL，并完善 SDK 与控制面的对接逻辑。完成后请停止。