package com.lumina.rpc.core.annotation;

import java.lang.annotation.*;

/**
 * Lumina RPC 服务提供者注解
 *
 * 标注在 Provider 的实现类上，用于暴露服务
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LuminaService {

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
     * 服务权重，用于负载均衡
     *
     * @return 权重值
     */
    int weight() default 100;
}
