package com.lumina.rpc.core.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡器
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private static final Logger logger = LoggerFactory.getLogger(RoundRobinLoadBalancer.class);

    // 每个服务的轮询计数器
    private final ConcurrentHashMap<String, AtomicInteger> serviceCounters = new ConcurrentHashMap<>();

    @Override
    public InetSocketAddress select(List<InetSocketAddress> serviceAddresses, String serviceName) {
        if (serviceAddresses == null || serviceAddresses.isEmpty()) {
            logger.warn("No available service addresses for service: {}", serviceName);
            return null;
        }

        if (serviceAddresses.size() == 1) {
            return serviceAddresses.get(0);
        }

        // 获取或创建该服务的计数器
        AtomicInteger counter = serviceCounters.computeIfAbsent(serviceName, k -> new AtomicInteger(0));

        // 原子性地获取并递增计数器
        int index = counter.getAndIncrement() % serviceAddresses.size();
        if (index < 0) {
            // 处理整数溢出的情况
            counter.set(0);
            index = 0;
        }

        InetSocketAddress selected = serviceAddresses.get(index);

        if (logger.isDebugEnabled()) {
            logger.debug("Selected address {} for service {} (index: {}, total: {})",
                    selected, serviceName, index, serviceAddresses.size());
        }

        return selected;
    }

    @Override
    public String getName() {
        return "round-robin";
    }
}
