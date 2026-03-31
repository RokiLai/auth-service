package com.example.authcenter.grpc.service;

import com.example.authcenter.grpc.ConfigDebugServiceGrpc;
import com.example.authcenter.grpc.ConsulConfigReply;
import com.example.authcenter.grpc.ConsulConfigRequest;
import com.example.authcenter.grpc.support.GrpcServiceSupport;
import io.grpc.stub.StreamObserver;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class ConfigDebugGrpcService extends ConfigDebugServiceGrpc.ConfigDebugServiceImplBase {

    private final Environment environment;

    public ConfigDebugGrpcService(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void getConsulConfig(ConsulConfigRequest request, StreamObserver<ConsulConfigReply> responseObserver) {
        GrpcServiceSupport.unary(responseObserver, () -> ConsulConfigReply.newBuilder()
                .setApplicationName(value(environment.getProperty("spring.application.name")))
                .setActiveProfile(value(environment.getProperty("spring.profiles.active")))
                .setDemoMessage(value(environment.getProperty("demo.message", "local-default")))
                .setSampleFlag(value(environment.getProperty("sample.flag", "undefined")))
                .build());
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
