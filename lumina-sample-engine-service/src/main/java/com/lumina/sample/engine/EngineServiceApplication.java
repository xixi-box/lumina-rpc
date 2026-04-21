package com.lumina.sample.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 曲率引擎传感器服务 - 启动类
 * Starfleet Demo: Warp Engine Sensor Node
 */
@SpringBootApplication(scanBasePackages = {"com.lumina.sample.engine", "com.lumina.rpc"})
public class EngineServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(EngineServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EngineServiceApplication.class, args);
        logger.info("✨ [Starfleet] Warp Engine Sensor Online - Port 8081");
        logger.info("🚀 曲率引擎传感器已启动 - 模拟深空高延迟通信");
    }
}
