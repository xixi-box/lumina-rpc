package com.lumina.controlplane.controller;

import com.lumina.controlplane.service.SseBroadcastService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 提供服务状态查询接口
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final SseBroadcastService sseBroadcastService;

    private final LocalDateTime startupTime = LocalDateTime.now();

    public HealthController(SseBroadcastService sseBroadcastService) {
        this.sseBroadcastService = sseBroadcastService;
    }

    /**
     * 健康检查
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "lumina-control-plane");
        health.put("startupTime", startupTime.toString());
        health.put("currentTime", LocalDateTime.now().toString());

        return ResponseEntity.ok(health);
    }

    /**
     * 就绪检查
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> ready = new HashMap<>();
        ready.put("status", "READY");
        ready.put("sseConnections", sseBroadcastService.getConnectionStats());

        return ResponseEntity.ok(ready);
    }

    /**
     * 存活检查
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> live() {
        Map<String, Object> live = new HashMap<>();
        live.put("status", "ALIVE");

        return ResponseEntity.ok(live);
    }
}
