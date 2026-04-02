package com.example.authcenter.config.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class GrpcServerConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(prefix = "grpc.server", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GrpcServerLifecycle grpcServerLifecycle(List<BindableService> bindableServices,
                                                   Environment environment) {
        int port = environment.getProperty("grpc.server.port", Integer.class, 9090);
        return new GrpcServerLifecycle(port, bindableServices);
    }

    public static class GrpcServerLifecycle {

        private static final Logger log = LoggerFactory.getLogger(GrpcServerLifecycle.class);

        private final int port;
        private final List<BindableService> services;
        private Server server;

        public GrpcServerLifecycle(int port, List<BindableService> services) {
            this.port = port;
            this.services = services;
        }

        public void start() throws IOException {
            NettyServerBuilder builder = NettyServerBuilder.forPort(port);
            services.forEach(builder::addService);
            server = builder.build();
            server.start();
            log.info("gRPC server started on port {} with {} service(s)", port, services.size());
        }

        public void stop() {
            if (server == null) {
                return;
            }
            server.shutdown();
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                server.shutdownNow();
            }
            log.info("gRPC server stopped");
        }
    }
}
