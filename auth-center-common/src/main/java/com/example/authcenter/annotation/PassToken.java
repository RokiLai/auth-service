package com.example.authcenter.annotation;

import java.lang.annotation.*;

@Target({ ElementType.METHOD, ElementType.TYPE })  // 方法或类上都可用
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PassToken {
    boolean required() default true;
}
