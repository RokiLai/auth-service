package com.example.authcenter.config;

import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;


@Component
public class RedisConnectionLogger {

    private final RedisConnectionFactory redisConnectionFactory;

    public RedisConnectionLogger(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @PostConstruct
    public void log() {
        System.out.println("RedisConnectionFactory class: " + redisConnectionFactory.getClass());
        // 如果是 LettuceConnectionFactory，可以强转打印 host
        if (redisConnectionFactory instanceof org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) {
            org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory factory =
                    (org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) redisConnectionFactory;
            System.out.println("Redis host: " + factory.getHostName());
            System.out.println("Redis port: " + factory.getPort());
        }
    }
}
