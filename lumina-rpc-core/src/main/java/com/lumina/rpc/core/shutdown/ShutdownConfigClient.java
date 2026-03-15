package com.lumina.rpc.core.shutdown;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 优雅停机配置客户端
 *
 * 改进：通过 SSE 接收停机信号，而非轮询
 * - 实时性更好（毫秒级 vs 秒级）
 * - 避免服务重启后误触发（停机信号只存在控制平面内存中）
 */
public class ShutdownConfigClient {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownConfigClient.class);

    private static final String CONTROL_PLANE_URL = "http://127.0.0.1:8080";

    private static ShutdownConfigClient instance;

    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService heartbeatScheduler;
    private final AtomicReference<ShutdownConfig> configRef;

    private String serviceName;
    private volatile boolean started = false;

    // SSE 连接状态
    private static HttpURLConnection sseConnection;
    private static Thread sseReaderThread;
    private static final AtomicBoolean connecting = new AtomicBoolean(false);

    /** 停机配置 */
    public static class ShutdownConfig {
        public long timeoutMs;
        public boolean enabled;

        public ShutdownConfig(long timeoutMs, boolean enabled) {
            this.timeoutMs = timeoutMs;
            this.enabled = enabled;
        }
    }

    private ShutdownConfigClient() {
        this.objectMapper = new ObjectMapper();
        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "shutdown-heartbeat");
            t.setDaemon(true);
            return t;
        });
        this.configRef = new AtomicReference<>(new ShutdownConfig(10000, true));
    }

    public static synchronized ShutdownConfigClient getInstance() {
        if (instance == null) {
            instance = new ShutdownConfigClient();
        }
        return instance;
    }

    /**
     * 启动配置同步
     *
     * 双通道模式：
     * 1. SSE 接收停机信号（实时，毫秒级）
     * 2. 定时心跳上报活跃请求数（用于前端显示）
     */
    public void start(String serviceName) {
        if (started) {
            return;
        }

        this.serviceName = serviceName;
        this.started = true;

        // 启动 SSE 长连接（接收停机信号）
        startSseLongConnection();

        // 每 5 秒上报活跃请求数（用于前端显示）
        heartbeatScheduler.scheduleAtFixedRate(this::reportActiveRequests, 5, 5, TimeUnit.SECONDS);

        logger.info("🔄 [ShutdownConfigClient] Started for service: {} (SSE + heartbeat)", serviceName);
    }

    /**
     * 启动 SSE 长连接 - 不断读取流
     */
    private void startSseLongConnection() {
        if (connecting.compareAndSet(false, true)) {
            sseReaderThread = new Thread(() -> {
                while (started && !Thread.currentThread().isInterrupted()) {
                    try {
                        connectAndReadStream();
                    } catch (Exception e) {
                        logger.debug("📡 [Shutdown-SSE] Connection error: {}", e.getMessage());
                    } finally {
                        // 连接断开，3 秒后重连
                        if (started) {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            }, "shutdown-sse-reader");
            sseReaderThread.setDaemon(true);
            sseReaderThread.start();

            connecting.set(false);
        }
    }

    /**
     * 连接并持续读取 SSE 流
     */
    private void connectAndReadStream() throws Exception {
        String sseUrl = CONTROL_PLANE_URL + "/api/v1/sse/subscribe/" + urlEncode(serviceName);
        logger.info("📡 [Shutdown-SSE] Connecting to: {}", sseUrl);

        URL url = new URL(sseUrl);
        sseConnection = (HttpURLConnection) url.openConnection();
        sseConnection.setRequestMethod("GET");
        sseConnection.setRequestProperty("Accept", "text/event-stream");
        sseConnection.setRequestProperty("Cache-Control", "no-cache");
        sseConnection.setConnectTimeout(10000);
        sseConnection.setReadTimeout(60 * 1000);
        sseConnection.setDoInput(true);

        int responseCode = sseConnection.getResponseCode();
        if (responseCode != 200) {
            logger.warn("📡 [Shutdown-SSE] Connection failed, HTTP {}", responseCode);
            sseConnection.disconnect();
            return;
        }

        logger.info("✅ [Shutdown-SSE] Connected, listening for shutdown signals...");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(sseConnection.getInputStream(), "UTF-8"))) {

            String line;
            String eventType = null;
            StringBuilder eventData = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                // 解析 SSE 事件格式
                if (line.startsWith("event:")) {
                    eventType = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    if (eventData.length() > 0) {
                        eventData.append("\n");
                    }
                    eventData.append(line.substring(5).trim());
                } else if (line.isEmpty() && eventType != null && eventData.length() > 0) {
                    // 空行分隔事件
                    String data = eventData.toString();
                    handleSseEvent(eventType, data);

                    eventType = null;
                    eventData.setLength(0);
                }
            }
        }

        logger.debug("📡 [Shutdown-SSE] Stream ended");
    }

    /**
     * 处理 SSE 事件
     */
    private void handleSseEvent(String eventType, String eventData) {
        switch (eventType) {
            case "connected":
                logger.info("✅ [Shutdown-SSE] SSE connection confirmed for service: {}", serviceName);
                break;

            case "heartbeat":
                // 心跳，忽略
                break;

            case "shutdown":
                try {
                    logger.warn("🛑 [Shutdown-SSE] Received shutdown signal from Control Plane!");

                    Map<String, Object> event = objectMapper.readValue(eventData, Map.class);
                    long timeoutMs = event.get("timeoutMs") != null
                            ? ((Number) event.get("timeoutMs")).longValue()
                            : 10000L;

                    // 更新超时时间
                    GracefulShutdownManager.getInstance().setShutdownTimeout(timeoutMs);

                    // 触发停机
                    GracefulShutdownManager.getInstance().gracefulShutdown();

                } catch (Exception e) {
                    logger.error("❌ [Shutdown-SSE] Failed to parse shutdown event: {}", e.getMessage());
                }
                break;

            default:
                logger.debug("📡 [Shutdown-SSE] Unknown event: {}", eventType);
        }
    }

    /**
     * 上报活跃请求数（用于前端显示）
     */
    private void reportActiveRequests() {
        if (serviceName == null) {
            return;
        }

        try {
            int activeRequests = GracefulShutdownManager.getInstance().getActiveRequestCount();

            Map<String, Object> body = Map.of("activeRequests", activeRequests);
            String jsonBody = objectMapper.writeValueAsString(body);

            URL url = new URL(CONTROL_PLANE_URL + "/api/v1/shutdown/configs/"
                    + urlEncode(serviceName) + "/heartbeat");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setDoOutput(true);
            conn.getOutputStream().write(jsonBody.getBytes("UTF-8"));

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    Map<String, Object> result = objectMapper.readValue(response.toString(), Map.class);
                    long timeoutMs = result.get("timeoutMs") != null
                            ? ((Number) result.get("timeoutMs")).longValue()
                            : 10000L;
                    boolean enabled = !Boolean.FALSE.equals(result.get("enabled"));

                    configRef.set(new ShutdownConfig(timeoutMs, enabled));
                    GracefulShutdownManager.getInstance().setShutdownTimeout(timeoutMs);
                }
            }

            conn.disconnect();

        } catch (Exception e) {
            logger.debug("📡 [Shutdown-Heartbeat] Failed: {}", e.getMessage());
        }
    }

    /**
     * 获取当前配置
     */
    public ShutdownConfig getConfig() {
        return configRef.get();
    }

    /**
     * 停止同步
     */
    public void stop() {
        if (!started) {
            return;
        }

        started = false;

        // 关闭 SSE 连接
        if (sseConnection != null) {
            try {
                sseConnection.disconnect();
            } catch (Exception ignored) {}
        }

        // 关闭心跳调度器
        heartbeatScheduler.shutdown();

        logger.info("🛑 [ShutdownConfigClient] Stopped");
    }

    /**
     * URL 编码
     */
    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}