package com.lumina.rpc.core.annotation;

import java.lang.annotation.*;

/**
 * Lumina RPC 服务消费者注解
 *
 * 标注在 Consumer 的接口字段上，用于注入代理对象
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LuminaReference {

    /**
     * 服务接口类
     *
     * @return 服务接口 Class
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 服务版本号
     *
     * @return 版本号
     */
    String version() default "";

    /**
     * 调用超时时间（毫秒）
     *
     * @return 超时时间
     */
    long timeout() default 5000;

    /**
     * 重试次数
     *
     * @return 重试次数
     */
    int retries() default 3;

    /**
     * 负载均衡策略
     *
     * @return 负载均衡策略名称
     */
    String loadBalance() default "round-robin";
}
