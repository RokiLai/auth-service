package com.example.authcenter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Debug")
@RequestMapping("/debug/config")
public class ConsulConfigDebugController {

    private final Environment environment;

    @Value("${demo.message:local-default}")
    private String demoMessage;

    public ConsulConfigDebugController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/consul")
    @Operation(summary = "查看当前配置快照")
    public Map<String, Object> consul() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("applicationName", environment.getProperty("spring.application.name"));
        payload.put("activeProfile", environment.getProperty("spring.profiles.active"));
        payload.put("demoMessage", demoMessage);
        payload.put("sampleFlag", environment.getProperty("sample.flag", "undefined"));
        return payload;
    }
}
