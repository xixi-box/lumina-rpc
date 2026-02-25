package com.lumina.controlplane.controller;

import com.lumina.controlplane.service.SseBroadcastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * SSE 订阅控制器
 * Consumer 通过此接口建立长连接，接收 Mock Rule 变更通知
 */
@RestController
@RequestMapping("/api/v1/sse")
public class SseController {

    private static final Logger logger = LoggerFactory.getLogger(SseController.class);

    private final SseBroadcastService sseBroadcastService;

    public SseController(SseBroadcastService sseBroadcastService) {
        this.sseBroadcastService = sseBroadcastService;
    }

    /**
     * 订阅指定服务的 Mock Rule 变更通知
     *
     * @param serviceName 服务名称
     * @return SseEmitter 长连接
     */
    @GetMapping(value = "/subscribe/{serviceName}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable("serviceName") String serviceName) {
        logger.info("New SSE subscription request for service: {}", serviceName);
        return sseBroadcastService.createEmitter(serviceName);
    }

    /**
     * 订阅所有服务的 Mock Rule 变更通知（管理员/调试用途）
     *
     * @return SseEmitter 长连接
     */
    @GetMapping(value = "/subscribe/all", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeAll() {
        logger.info("New SSE subscription request for all services");
        return sseBroadcastService.createEmitter("__ALL__");
    }

    /**
     * 获取当前 SSE 连接统计
     */
    @GetMapping("/stats")
    public Map<String, Integer> getStats() {
        return sseBroadcastService.getConnectionStats();
    }
}
