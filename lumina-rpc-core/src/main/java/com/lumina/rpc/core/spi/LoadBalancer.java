package com.lumina.rpc.core.spi;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 负载均衡器接口
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
     * 获取负载均衡器名称
     *
     * @return 名称
     */
    String getName();
}
