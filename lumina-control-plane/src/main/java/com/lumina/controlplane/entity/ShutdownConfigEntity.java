package com.lumina.controlplane.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 优雅停机配置实体
 *
 * 只存储配置信息，停机信号（shuttingDown）改为内存存储
 * 原因：停机是瞬时操作，不应持久化导致服务重启后误触发
 */
@Entity
@Table(name = "shutdown_config")
public class ShutdownConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 服务名 */
    @Column(unique = true, nullable = false)
    private String serviceName;

    /** 是否启用优雅停机 */
    @Column(nullable = false)
    private Boolean enabled = true;

    /** 停机超时时间（毫秒） */
    @Column(nullable = false)
    private Long timeoutMs = 10000L;

    /** 正在处理的请求数（Provider 上报） */
    private Integer activeRequests = 0;

    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getActiveRequests() {
        return activeRequests;
    }

    public void setActiveRequests(Integer activeRequests) {
        this.activeRequests = activeRequests;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}