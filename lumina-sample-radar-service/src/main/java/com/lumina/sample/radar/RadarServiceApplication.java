package com.lumina.sample.radar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 深空雷达阵列服务 - 启动类
 * Starfleet Demo: Deep Space Radar Array Node (故障模拟节点)
 */
@SpringBootApplication(scanBasePackages = {"com.lumina.sample.radar", "com.lumina.rpc"})
public class RadarServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(RadarServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RadarServiceApplication.class, args);
        logger.info("📡 [Starfleet] Deep Space Radar Array Online - Port 8082");
        logger.info("🎯 深空雷达阵列已启动 - ⚠️ 故障模拟节点 (40%概率离子风暴干扰)");
    }
}
