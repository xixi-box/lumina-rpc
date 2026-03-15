package com.lumina.controlplane.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lumina_service_instance")
public class ServiceInstanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, length = 255)
    private String serviceName;

    @Column(name = "instance_id", nullable = false, unique = true, length = 255)
    private String instanceId;

    @Column(name = "host", nullable = false, length = 255)
    private String host;

    @Column(name = "port", nullable = false)
    private Integer port;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "UP";

    @Column(name = "version", length = 50)
    private String version;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 服务元数据 - JSON 格式存储接口方法信息
     * 结构：{"methods":[{"name":"methodName","parameterTypes":["java.lang.String","int"]},...]}
     */
    @Column(name = "service_metadata", columnDefinition = "TEXT")
    private String serviceMetadata;

    /** 实例启动时间戳（毫秒），用于预热计算 */
    @Column(name = "start_time")
    private Long startTime;

    /** 预热时间（毫秒），默认 60 秒 */
    @Column(name = "warmup_period")
    private Long warmupPeriod = 60000L;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
        if (lastHeartbeat == null) {
            lastHeartbeat = LocalDateTime.now();
        }
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusSeconds(90); // 90秒过期（容忍 2 次心跳丢失）
        }
        // 如果没有设置 startTime，使用当前时间
        if (startTime == null) {
            startTime = System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // 高可用：只有在未显式设置 expiresAt 时才自动计算
        // ServiceInstanceService.heartbeat() 会显式设置 expiresAt = now + 90s
        if (expiresAt == null && lastHeartbeat != null) {
            expiresAt = lastHeartbeat.plusSeconds(90);
        }
    }

    /**
     * 计算预热权重
     *
     * @return 权重值 [0.0, 1.0]
     */
    public double getWarmupWeight() {
        if (warmupPeriod == null || warmupPeriod <= 0) {
            return 1.0;
        }
        if (startTime == null) {
            return 1.0;
        }

        long uptime = System.currentTimeMillis() - startTime;
        if (uptime >= warmupPeriod) {
            return 1.0;
        }

        return (double) uptime / warmupPeriod;
    }

    /**
     * 检查是否在预热期
     */
    public boolean isInWarmup() {
        if (warmupPeriod == null || warmupPeriod <= 0 || startTime == null) {
            return false;
        }
        return System.currentTimeMillis() - startTime < warmupPeriod;
    }

    /**
     * 获取预热进度百分比
     */
    public int getWarmupProgress() {
        if (warmupPeriod == null || warmupPeriod <= 0 || startTime == null) {
            return 100;
        }
        long uptime = System.currentTimeMillis() - startTime;
        int progress = (int) (uptime * 100 / warmupPeriod);
        return Math.min(100, Math.max(0, progress));
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getServiceMetadata() {
        return serviceMetadata;
    }

    public void setServiceMetadata(String serviceMetadata) {
        this.serviceMetadata = serviceMetadata;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getWarmupPeriod() {
        return warmupPeriod;
    }

    public void setWarmupPeriod(Long warmupPeriod) {
        this.warmupPeriod = warmupPeriod;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isHealthy() {
        return "UP".equals(status) && !isExpired();
    }
}
