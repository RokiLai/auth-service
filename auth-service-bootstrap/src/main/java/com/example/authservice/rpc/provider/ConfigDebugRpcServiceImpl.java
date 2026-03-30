package com.example.authservice.rpc.provider;

import com.example.authservice.rpc.api.ConfigDebugRpcService;
import com.example.authservice.rpc.dto.response.NacosConfigDebugRpcResponse;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;

@RefreshScope
@DubboService
public class ConfigDebugRpcServiceImpl implements ConfigDebugRpcService {

    private final Environment environment;

    @Value("${demo.message:local-default}")
    private String demoMessage;

    public ConfigDebugRpcServiceImpl(Environment environment) {
        this.environment = environment;
    }

    @Override
    public NacosConfigDebugRpcResponse nacosConfig() {
        return new NacosConfigDebugRpcResponse(
                environment.getProperty("spring.application.name"),
                environment.getProperty("spring.profiles.active"),
                demoMessage,
                environment.getProperty("sample.flag", "undefined")
        );
    }
}
