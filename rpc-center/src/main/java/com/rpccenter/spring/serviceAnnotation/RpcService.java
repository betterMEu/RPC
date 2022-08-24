package com.rpccenter.spring.serviceAnnotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author yls91
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
@Component
public @interface RpcService {

    String version() default "";

    String group() default "";
}
