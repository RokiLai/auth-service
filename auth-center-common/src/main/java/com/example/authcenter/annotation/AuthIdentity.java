package com.example.authcenter.annotation;

import java.lang.annotation.*;

/**
 * 标记需要由 MVC 参数解析器注入当前已认证身份。
 * Marks a controller parameter to be resolved as the current authenticated identity.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthIdentity {
}
