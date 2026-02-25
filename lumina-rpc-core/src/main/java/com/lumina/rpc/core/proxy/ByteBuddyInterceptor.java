package com.lumina.rpc.core.proxy;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.lang.reflect.Method;

/**
 * ByteBuddy 拦截器
 *
 * 拦截接口方法调用，委托给 RpcClientHandler 处理
 */
public class ByteBuddyInterceptor {

    // RPC 客户端处理器
    private final RpcClientHandler clientHandler;

    public ByteBuddyInterceptor(RpcClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    /**
     * 拦截方法调用
     *
     * @param method 被调用的方法
     * @param args   方法参数
     * @return 方法返回值
     * @throws Throwable 调用异常
     */
    @RuntimeType
    public Object intercept(@Origin Method method, @AllArguments Object[] args) throws Throwable {
        // 将方法调用委托给 RpcClientHandler
        return clientHandler.invoke(null, method, args);
    }
}
