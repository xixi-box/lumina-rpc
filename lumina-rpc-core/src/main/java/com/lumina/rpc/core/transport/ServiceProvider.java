package com.lumina.rpc.core.transport;

import com.lumina.rpc.core.annotation.LuminaService;
import com.lumina.rpc.core.spi.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务提供者
 *
 * 封装了服务注册和服务器启动的功能
 */
public class ServiceProvider {

    private static final Logger logger = LoggerFactory.getLogger(ServiceProvider.class);

    // Netty 服务器
    private final NettyServer nettyServer;

    // 服务注册表
    private final ServiceRegistry serviceRegistry;

    // 序列化器
    private final Serializer serializer;

    // 端口
    private final int port;

    public ServiceProvider(int port, Serializer serializer) {
        this.port = port;
        this.serializer = serializer;
        this.serviceRegistry = new ServiceRegistry();
        // 创建请求处理器
        DefaultRpcRequestHandler requestHandler = new DefaultRpcRequestHandler(serviceRegistry);
        this.nettyServer = new NettyServer(serializer, requestHandler);
    }

    /**
     * 发布服务
     *
     * @param serviceBean 服务实现实例
     */
    public void publishService(Object serviceBean) {
        // 获取服务类上的 @LuminaService 注解
        LuminaService serviceAnnotation = serviceBean.getClass().getAnnotation(LuminaService.class);

        Class<?> interfaceClass;
        String version;

        if (serviceAnnotation != null) {
            // 从注解获取配置
            interfaceClass = serviceAnnotation.interfaceClass();
            if (interfaceClass == void.class) {
                // 如果没有指定接口，尝试获取实现的第一个接口
                Class<?>[] interfaces = serviceBean.getClass().getInterfaces();
                if (interfaces.length > 0) {
                    interfaceClass = interfaces[0];
                } else {
                    throw new IllegalArgumentException("Service must implement an interface: " +
                            serviceBean.getClass().getName());
                }
            }
            version = serviceAnnotation.version();
        } else {
            // 没有注解，自动推断
            Class<?>[] interfaces = serviceBean.getClass().getInterfaces();
            if (interfaces.length > 0) {
                interfaceClass = interfaces[0];
            } else {
                throw new IllegalArgumentException("Service must implement an interface: " +
                        serviceBean.getClass().getName());
            }
            version = "";
        }

        // 注册服务
        String interfaceName = interfaceClass.getName();
        serviceRegistry.registerService(interfaceName, version, serviceBean);

        logger.info("Published service: interface={}, version={}, implementation={}",
                interfaceName, version, serviceBean.getClass().getName());
    }

    /**
     * 注册服务（手动指定接口和版本）
     *
     * @param interfaceClass 接口类
     * @param version        版本号
     * @param serviceBean    服务实例
     */
    public void registerService(Class<?> interfaceClass, String version, Object serviceBean) {
        serviceRegistry.registerService(interfaceClass.getName(), version, serviceBean);
        logger.info("Registered service: interface={}, version={}, implementation={}",
                interfaceClass.getName(), version, serviceBean.getClass().getName());
    }

    /**
     * 启动服务器
     */
    public void start() {
        // 在新线程中启动服务器，避免阻塞
        new Thread(() -> nettyServer.start(port), "rpc-server-starter").start();
    }

    /**
     * 关闭服务提供者
     */
    public void shutdown() {
        logger.info("Shutting down service provider...");
        nettyServer.shutdown();
        serviceRegistry.clear();
        logger.info("Service provider shutdown complete");
    }

    /**
     * 获取端口号
     *
     * @return 端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 获取服务注册表
     *
     * @return 服务注册表
     */
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }
}
