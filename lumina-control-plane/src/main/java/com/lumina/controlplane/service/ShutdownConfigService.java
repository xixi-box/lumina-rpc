package com.lumina.controlplane.service;

import com.lumina.controlplane.entity.ShutdownConfigEntity;
import com.lumina.controlplane.repository.ShutdownConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 优雅停机配置服务
 *
 * 核心改进：停机信号改为内存存储，通过 SSE 实时推送
 * - 避免服务重启后误触发停机
 * - 停机是瞬时操作，不应持久化
 */
@Service
public class ShutdownConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownConfigService.class);

    private final ShutdownConfigRepository repository;
    private final SseBroadcastService sseBroadcastService;

    /** 停机信号内存存储 - serviceName -> 是否正在停机 */
    private final ConcurrentHashMap<String, Boolean> shutdownSignals = new ConcurrentHashMap<>();

    public ShutdownConfigService(ShutdownConfigRepository repository, SseBroadcastService sseBroadcastService) {
        this.repository = repository;
        this.sseBroadcastService = sseBroadcastService;
    }

    /**
     * 获取所有配置
     */
    public List<ShutdownConfigEntity> findAll() {
        return repository.findAll();
    }

    /**
     * 获取指定服务的配置
     */
    public ShutdownConfigEntity findByServiceName(String serviceName) {
        return repository.findByServiceName(serviceName).orElse(null);
    }

    /**
     * 获取或创建配置
     */
    @Transactional
    public ShutdownConfigEntity getOrCreate(String serviceName) {
        Optional<ShutdownConfigEntity> existing = repository.findByServiceName(serviceName);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 创建默认配置
        ShutdownConfigEntity config = new ShutdownConfigEntity();
        config.setServiceName(serviceName);
        config.setEnabled(true);
        config.setTimeoutMs(10000L);
        config.setActiveRequests(0);

        return repository.save(config);
    }

    /**
     * 更新停机配置
     */
    @Transactional
    public ShutdownConfigEntity updateConfig(String serviceName, Boolean enabled, Long timeoutMs) {
        ShutdownConfigEntity config = getOrCreate(serviceName);

        if (enabled != null) {
            config.setEnabled(enabled);
        }
        if (timeoutMs != null) {
            config.setTimeoutMs(timeoutMs);
        }

        return repository.save(config);
    }

    /**
     * 触发停机
     *
     * 关键改进：只存内存，并通过 SSE 实时推送
     */
    public ShutdownConfigEntity triggerShutdown(String serviceName) {
        // 1. 设置内存中的停机信号
        shutdownSignals.put(serviceName, true);

        // 2. 获取配置（用于返回和推送）
        ShutdownConfigEntity config = getOrCreate(serviceName);

        // 3. 通过 SSE 推送停机信号给 Provider
        sseBroadcastService.broadcastShutdownSignal(serviceName, config.getTimeoutMs());

        logger.info("🛑 Shutdown triggered for service: {} (SSE pushed, timeout: {}ms)",
                serviceName, config.getTimeoutMs());

        return config;
    }

    /**
     * 取消停机
     */
    public ShutdownConfigEntity cancelShutdown(String serviceName) {
        // 1. 清除内存中的停机信号
        Boolean removed = shutdownSignals.remove(serviceName);

        if (removed != null) {
            logger.info("✅ Shutdown signal cleared for service: {}", serviceName);
        }

        // 2. 返回配置
        return repository.findByServiceName(serviceName).orElse(null);
    }

    /**
     * 检查服务是否在停机中（内存查询，非数据库）
     */
    public boolean isShuttingDown(String serviceName) {
        return Boolean.TRUE.equals(shutdownSignals.get(serviceName));
    }

    /**
     * 更新活跃请求数（Provider 上报）
     */
    @Transactional
    public void updateActiveRequests(String serviceName, int activeRequests) {
        ShutdownConfigEntity config = getOrCreate(serviceName);
        config.setActiveRequests(activeRequests);
        repository.save(config);
    }

    /**
     * 删除配置
     */
    @Transactional
    public void delete(String serviceName) {
        shutdownSignals.remove(serviceName);
        repository.deleteByServiceName(serviceName);
    }

    /**
     * 获取停机状态概览
     */
    public ShutdownStatusOverview getStatusOverview() {
        List<ShutdownConfigEntity> configs = repository.findAll();

        int totalServices = configs.size();
        int shuttingDown = 0;
        int running = 0;
        int totalActiveRequests = 0;

        for (ShutdownConfigEntity config : configs) {
            // 从内存判断停机状态
            if (isShuttingDown(config.getServiceName())) {
                shuttingDown++;
            } else {
                running++;
            }
            if (config.getActiveRequests() != null) {
                totalActiveRequests += config.getActiveRequests();
            }
        }

        return new ShutdownStatusOverview(totalServices, running, shuttingDown, totalActiveRequests);
    }

    public static class ShutdownStatusOverview {
        public final int totalServices;
        public final int running;
        public final int shuttingDown;
        public final int totalActiveRequests;

        public ShutdownStatusOverview(int totalServices, int running, int shuttingDown, int totalActiveRequests) {
            this.totalServices = totalServices;
            this.running = running;
            this.shuttingDown = shuttingDown;
            this.totalActiveRequests = totalActiveRequests;
        }
    }
}