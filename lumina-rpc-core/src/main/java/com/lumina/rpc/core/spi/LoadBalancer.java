package com.lumina.rpc.core.spi;

import com.lumina.rpc.core.discovery.ServiceInstance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 负载均衡器接口
 *
 * 支持预热权重：
 * - 新实例启动后权重从 0 逐渐增加到 1
 * - 避免瞬时高负载导致超时
 */
public interface LoadBalancer {

    /**
     * 从可用服务地址列表中选择一个地址
     *
     * @param serviceAddresses 可用服务地址列表
     * @param serviceName      服务名称
     * @return 选中的服务地址
     */
    InetSocketAddress select(List<InetSocketAddress> serviceAddresses, String serviceName);

    /**
     * 从可用服务实例列表中选择一个地址（支持预热权重）
     *
     * @param instances   可用服务实例列表
     * @param serviceName 服务名称
     * @return 选中的服务地址
     */
    default InetSocketAddress selectInstance(List<ServiceInstance> instances, String serviceName) {
        // 默认实现：忽略预热权重，转换为地址列表
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        List<InetSocketAddress> addresses = new java.util.ArrayList<>();
        for (ServiceInstance instance : instances) {
            addresses.add(new InetSocketAddress(instance.getHost(), instance.getPort()));
        }
        return select(addresses, serviceName);
    }

    /**
     * 获取负载均衡器名称
     *
     * @return 名称
     */
    String getName();
}
