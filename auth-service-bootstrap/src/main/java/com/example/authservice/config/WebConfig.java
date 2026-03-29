package com.example.authservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;
    private final CurrentIdentityArgumentResolver currentIdentityArgumentResolver;

    public WebConfig(JwtInterceptor jwtInterceptor,
                     CurrentIdentityArgumentResolver currentIdentityArgumentResolver) {
        this.jwtInterceptor = jwtInterceptor;
        this.currentIdentityArgumentResolver = currentIdentityArgumentResolver;
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
        // 注册当前身份参数解析器，使控制器可以直接声明 @AuthIdentity CurrentIdentity。
        // Registers the identity argument resolver so controllers can declare @AuthIdentity CurrentIdentity directly.
        resolvers.add(currentIdentityArgumentResolver);
    }

}
