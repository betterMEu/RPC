package com.rpccenter.spring.serviceAnnotation;

import java.lang.annotation.*;

/**
 * @author yls91
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
public @interface RpcReference {

    String version() default "";

    String group() default "";
}