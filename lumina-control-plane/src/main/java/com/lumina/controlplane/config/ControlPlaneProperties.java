package com.lumina.controlplane.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lumina.control-plane")
public class ControlPlaneProperties {

    private final Registry registry = new Registry();
    private final Sse sse = new Sse();

    public Registry getRegistry() {
        return registry;
    }

    public Sse getSse() {
        return sse;
    }

    public static class Registry {
        private long instanceTtlSeconds = 90;
        private long cleanupFixedRateMs = 60000;
        private long zombieRetentionHours = 1;

        public long getInstanceTtlSeconds() {
            return instanceTtlSeconds;
        }

        public void setInstanceTtlSeconds(long instanceTtlSeconds) {
            this.instanceTtlSeconds = instanceTtlSeconds;
        }

        public long getCleanupFixedRateMs() {
            return cleanupFixedRateMs;
        }

        public void setCleanupFixedRateMs(long cleanupFixedRateMs) {
            this.cleanupFixedRateMs = cleanupFixedRateMs;
        }

        public long getZombieRetentionHours() {
            return zombieRetentionHours;
        }

        public void setZombieRetentionHours(long zombieRetentionHours) {
            this.zombieRetentionHours = zombieRetentionHours;
        }
    }

    public static class Sse {
        private long timeoutMs = 1800000;
        private long heartbeatIntervalSeconds = 30;

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public long getHeartbeatIntervalSeconds() {
            return heartbeatIntervalSeconds;
        }

        public void setHeartbeatIntervalSeconds(long heartbeatIntervalSeconds) {
            this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        }
    }
}
