package com.example.authcenter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;
    private final CurrentOperatorArgumentResolver currentOperatorArgumentResolver;

    public WebConfig(JwtInterceptor jwtInterceptor,
                     CurrentOperatorArgumentResolver currentOperatorArgumentResolver) {
        this.jwtInterceptor = jwtInterceptor;
        this.currentOperatorArgumentResolver = currentOperatorArgumentResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/error"); // 拦截所有请求

    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 允许所有路径
                .allowedOriginPatterns("*") // 允许任意源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 注册当前操作者参数解析器，使控制器可以直接声明 @AuthIdentity CurrentOperator。
        // Registers the current-operator argument resolver so controllers can declare @AuthIdentity CurrentOperator directly.
        resolvers.add(currentOperatorArgumentResolver);
    }

}
