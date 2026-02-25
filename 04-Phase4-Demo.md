# Phase 4: 构建 Sample 业务节点 (闭环演示)

## 1. 目标
写两个业务微服务，充当“小白鼠”，让整个集群活起来。不涉及任何复杂的外部网关，Consumer 兼任简易 BFF。

## 2. `lumina-sample-provider` 要求
- 引入 Spring Boot Web 和 `lumina-rpc-core`。
- 实现 `UserService.getUser(Long uid)`，使用 `@LuminaService` 暴露。
- **特效：** 方法内部使用 `Thread.sleep(100 ~ 500)` 模拟数据库慢查询，并打印耗时日志。配置 application.yml 连向控制中心。

## 3. `lumina-sample-consumer` 要求 (兼任 BFF 网关)
- 引入 Spring Boot Web 和 `lumina-rpc-core`。
- 使用 `@LuminaReference` 注入 `UserService`。
- **自动流量引擎：** 写一个 `@Scheduled(fixedRate = 3000)` 定时任务，每 3 秒自动发起一次 RPC 调用并打印结果。
- **手动 API 网关接口：** 额外暴露一个 Spring MVC 接口 `GET /api/test/user/{uid}`。当用浏览器访问时，在 Controller 里发起 RPC 调用并返回给前端浏览器。这证明了 RPC 可以被外部 HTTP 流量平滑触发。

## 4. 输出动作
完成两个 Sample 工程，确保它们能够成功注册到 Control Plane，并在前端拓扑图上亮起连线。