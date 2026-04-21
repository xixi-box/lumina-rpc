package com.lumina.rpc.core.spi;

import com.lumina.rpc.core.discovery.ServiceInstance;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RoundRobinLoadBalancerTest {

    @Test
    void excludesFailedAddressesBeforeSelecting() {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer();
        ServiceInstance first = instance("127.0.0.1", 9001);
        ServiceInstance second = instance("127.0.0.1", 9002);

        SelectionResult result = loadBalancer.selectWithExclusion(
                List.of(first, second),
                List.of(new InetSocketAddress("127.0.0.1", 9001)),
                "demo.Service",
                null);

        assertEquals(new InetSocketAddress("127.0.0.1", 9002), result.getAddress());
    }

    @Test
    void returnsNullWhenAllInstancesAreExcluded() {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer();
        ServiceInstance only = instance("127.0.0.1", 9001);

        SelectionResult result = loadBalancer.selectWithExclusion(
                List.of(only),
                List.of(new InetSocketAddress("127.0.0.1", 9001)),
                "demo.Service",
                null);

        assertNull(result);
    }

    private ServiceInstance instance(String host, int port) {
        ServiceInstance instance = new ServiceInstance("demo.Service", host, port);
        instance.setWarmupPeriod(0);
        return instance;
    }
}
