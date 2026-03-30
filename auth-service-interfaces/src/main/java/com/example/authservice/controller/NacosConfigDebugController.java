package com.example.authservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RefreshScope
@RestController
@RequestMapping("/debug/config")
public class NacosConfigDebugController {

    private final Environment environment;

    @Value("${demo.message:local-default}")
    private String demoMessage;

    public NacosConfigDebugController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/nacos")
    public Map<String, Object> nacos() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("applicationName", environment.getProperty("spring.application.name"));
        payload.put("activeProfile", environment.getProperty("spring.profiles.active"));
        payload.put("demoMessage", demoMessage);
        payload.put("sampleFlag", environment.getProperty("sample.flag", "undefined"));
        return payload;
    }
}
