# Phase 1: 构建 `lumina-rpc-core` (高性能底层 SDK)

## 1. 目标
完成 RPC 的底层通信基础，屏蔽网络细节，实现本地调用的假象。

## 2. 技术栈
Java 21, Netty 4, ByteBuddy, 纯 Java SPI。

## 3. 核心功能点要求
### 3.1 自定义通信协议 (Netty)
- 必须自定义消息协议：`Magic Number (2 byte) + Version (1 byte) + Serializer Type (1 byte) + Message Type (Request/Response) (1 byte) + Request ID (8 byte) + Data Length (4 byte) + Body Byte Array`。
- 实现 Netty 的 Encoder/Decoder，解决 TCP 粘包和半包问题 (`LengthFieldBasedFrameDecoder`)。
- 维护一个全局的 `ConcurrentHashMap<Long, CompletableFuture<RpcResponse>>` 用于将异步的 Netty 响应转为同步的方法返回。

### 3.2 自定义注解
- `@LuminaService`: 打在 Provider 的实现类上，用于暴露服务。
- `@LuminaReference`: 打在 Consumer 的接口字段上，用于注入代理对象。

### 3.3 动态代理与 SPI
- 使用 `ByteBuddy` 拦截被 `@LuminaReference` 标注的接口，将其方法调用（类名、方法名、参数类型、参数值）封装为 `RpcRequest`。
- 定义 `Serializer` 和 `LoadBalancer` 接口。使用 Java SPI 机制（`META-INF/services`）实现默认的 JSON 序列化和轮询负载均衡。

## 4. 输出动作
请初始化 Maven 父工程，并在 `lumina-rpc-core` 中完成上述所有底层代码。不包含任何与注册中心的交互。完成后请停止。