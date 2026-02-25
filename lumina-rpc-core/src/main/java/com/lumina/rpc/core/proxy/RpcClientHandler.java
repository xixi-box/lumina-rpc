package com.lumina.rpc.core.proxy;

import com.lumina.rpc.core.common.PendingRequestManager;
import com.lumina.rpc.core.common.RequestIdGenerator;
import com.lumina.rpc.core.protocol.RpcMessage;
import com.lumina.rpc.core.protocol.RpcRequest;
import com.lumina.rpc.core.protocol.RpcResponse;
import com.lumina.rpc.core.spi.Serializer;
import com.lumina.rpc.core.transport.NettyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * RPC 客户端动态代理处理器
 *
 * 拦截被 @LuminaReference 标注的接口方法调用，封装为 RpcRequest 并发送
 */
public class RpcClientHandler implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);

    // 服务接口类
    private final Class<?> interfaceClass;

    // 服务版本号
    private final String version;

    // 超时时间（毫秒）
    private final long timeout;

    // 序列化器
    private final Serializer serializer;

    // Netty 客户端
    private final NettyClient nettyClient;

    // 请求ID生成器
    private final RequestIdGenerator requestIdGenerator;

    // 待处理请求管理器
    private final PendingRequestManager pendingRequestManager;

    public RpcClientHandler(Class<?> interfaceClass, String version, long timeout,
                            Serializer serializer, NettyClient nettyClient) {
        this.interfaceClass = interfaceClass;
        this.version = version != null ? version : "";
        this.timeout = timeout > 0 ? timeout : 5000;
        this.serializer = serializer;
        this.nettyClient = nettyClient;
        this.requestIdGenerator = RequestIdGenerator.getInstance();
        this.pendingRequestManager = PendingRequestManager.getInstance();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 处理 Object 类的方法（如 toString, hashCode, equals）
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        // 构建 RpcRequest
        RpcRequest request = buildRpcRequest(method, args);

        // 发送请求并等待响应
        return sendRequest(request);
    }

    /**
     * 构建 RPC 请求对象
     *
     * @param method 方法
     * @param args   参数
     * @return RpcRequest
     */
    private RpcRequest buildRpcRequest(Method method, Object[] args) {
        RpcRequest request = new RpcRequest();
        request.setRequestId(requestIdGenerator.nextId());
        request.setInterfaceName(interfaceClass.getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args != null ? args : new Object[0]);
        request.setVersion(version);
        return request;
    }

    /**
     * 发送 RPC 请求
     *
     * @param request RPC 请求
     * @return 方法返回值
     * @throws Exception 调用异常
     */
    private Object sendRequest(RpcRequest request) throws Exception {
        // 构建 RpcMessage
        RpcMessage message = new RpcMessage();
        message.setMagicNumber(RpcMessage.MAGIC_NUMBER);
        message.setVersion(RpcMessage.VERSION);
        message.setSerializerType(serializer.getType());
        message.setMessageType(RpcMessage.REQUEST);
        message.setRequestId(request.getRequestId());
        message.setBody(request);

        // 创建 CompletableFuture 用于等待响应
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        pendingRequestManager.addPendingRequest(request.getRequestId(), future);

        try {
            // 发送消息
            if (logger.isDebugEnabled()) {
                logger.debug("Sending RPC request: {}", request);
            }
            nettyClient.sendMessage(message);

            // 等待响应
            RpcResponse response = future.get(timeout, TimeUnit.MILLISECONDS);

            // 处理响应
            if (response == null) {
                throw new RuntimeException("RPC response is null");
            }

            if (!response.isSuccess()) {
                throw new RuntimeException("RPC call failed: " + response.getMessage());
            }

            return response.getData();

        } finally {
            // 确保从待处理请求中移除
            pendingRequestManager.removePendingRequest(request.getRequestId());
        }
    }
}
