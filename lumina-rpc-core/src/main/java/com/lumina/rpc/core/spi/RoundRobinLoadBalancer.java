package com.lumina.rpc.core.spi;

import com.lumina.rpc.core.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡器
 *
 * 支持服务预热：
 * - 新实例启动后，权重从 0 逐渐增加到 1
 * - 预热期间的实例被选中的概率更低
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

    /**
     * 从服务实例列表中选择（支持预热权重）
     *
     * 算法：加权随机选择
     * - 预热中的实例权重较低，被选中的概率也较低
     * - 预热完成的实例权重为 1.0，正常接收流量
     */
    @Override
    public InetSocketAddress selectInstance(List<ServiceInstance> instances, String serviceName) {
        if (instances == null || instances.isEmpty()) {
            logger.warn("No available service instances for service: {}", serviceName);
            return null;
        }

        if (instances.size() == 1) {
            ServiceInstance instance = instances.get(0);
            return new InetSocketAddress(instance.getHost(), instance.getPort());
        }

        // 计算每个实例的预热权重
        double[] weights = new double[instances.size()];
        double totalWeight = 0.0;

        for (int i = 0; i < instances.size(); i++) {
            ServiceInstance instance = instances.get(i);
            double weight = instance.getWarmupWeight();
            weights[i] = weight;
            totalWeight += weight;

            if (logger.isDebugEnabled() && instance.isInWarmup()) {
                logger.debug("[Warmup] Instance {}:{} weight={:.2f}, progress={}%",
                        instance.getHost(), instance.getPort(), weight, instance.getWarmupProgress());
            }
        }

        // 如果总权重为 0（所有实例都在预热最开始），给所有实例相等的权重
        if (totalWeight <= 0) {
            totalWeight = instances.size();
            for (int i = 0; i < weights.length; i++) {
                weights[i] = 1.0;
            }
        }

        // 加权随机选择
        double random = Math.random() * totalWeight;
        double cumulative = 0.0;

        for (int i = 0; i < instances.size(); i++) {
            cumulative += weights[i];
            if (random < cumulative) {
                ServiceInstance selected = instances.get(i);

                if (logger.isDebugEnabled()) {
                    logger.debug("[LoadBalancer] Selected {}:{} for service {} (weight={:.2f}, warmup={})",
                            selected.getHost(), selected.getPort(), serviceName,
                            weights[i], selected.isInWarmup());
                }

                return new InetSocketAddress(selected.getHost(), selected.getPort());
            }
        }

        // 兜底：返回最后一个实例
        ServiceInstance last = instances.get(instances.size() - 1);
        return new InetSocketAddress(last.getHost(), last.getPort());
    }

    @Override
    public String getName() {
        return "round-robin";
    }
}
