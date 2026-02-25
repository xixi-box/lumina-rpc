# Phase 3: 构建 `lumina-dashboard` (可视化面板)

## 1. 目标
打造一个炫酷的控制台，让面试官一眼看出你的工程不仅有底层，还有顶级的产品化包装。

## 2. 技术栈
Vue 3, Vite, TypeScript, TailwindCSS, Vue Flow (必须使用)。

## 3. 核心页面要求
### 3.1 全局拓扑视图 (Topology Map)
- 调用后台 API 获取当前所有注册的实例。
- 使用 `Vue Flow` 节点连线图，动态绘制 Provider 和 Consumer 的依赖网络（比如：Consumer A -> 连线 -> Provider B）。节点需显示 IP 和端口。

### 3.2 Mock 与降级规则配置页
- 列表展示所有可用的 RPC 接口。
- 点击某个接口，弹出一个抽屉（Drawer）或对话框，包含一个 JSON 编辑器。
- 管理员可以在这里手写 JSON 并点击保存。保存动作将调用 Control Plane 的 API，落入 MySQL，并瞬间通过 SSE 触发底层 Consumer 的降级拦截。

## 4. 输出动作
初始化前端工程，配置跨域请求，完成页面与后台 API 的对接。