package com.example.authcenter.config.consul;

import com.ecwid.consul.v1.agent.model.NewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "app.consul.rpc-registration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConsulRpcRegistrationConfiguration {

    @Bean
    public SmartLifecycle consulRpcRegistrationLifecycle(ConsulServiceRegistry serviceRegistry,
                                                         ConsulDiscoveryProperties discoveryProperties,
                                                         ApplicationContext applicationContext,
                                                         Environment environment) {
        return new ConsulRpcRegistrationLifecycle(serviceRegistry, discoveryProperties, applicationContext, environment);
    }

    static class ConsulRpcRegistrationLifecycle implements SmartLifecycle {

        private static final Logger log = LoggerFactory.getLogger(ConsulRpcRegistrationLifecycle.class);

        private final ConsulServiceRegistry serviceRegistry;
        private final ConsulDiscoveryProperties discoveryProperties;
        private final ApplicationContext applicationContext;
        private final Environment environment;

        private volatile boolean running;
        private volatile ConsulRegistration registration;

        ConsulRpcRegistrationLifecycle(ConsulServiceRegistry serviceRegistry,
                                       ConsulDiscoveryProperties discoveryProperties,
                                       ApplicationContext applicationContext,
                                       Environment environment) {
            this.serviceRegistry = serviceRegistry;
            this.discoveryProperties = discoveryProperties;
            this.applicationContext = applicationContext;
            this.environment = environment;
        }

        @Override
        public void start() {
            if (running || !discoveryProperties.isRegister()) {
                return;
            }

            registration = buildRegistration();
            serviceRegistry.register(registration);
            running = true;
            log.info("Registered RPC Consul service: name={}, id={}, address={}, port={}",
                    registration.getServiceId(), registration.getInstanceId(), registration.getHost(), registration.getPort());
        }

        @Override
        public void stop() {
            if (!running || registration == null) {
                return;
            }

            serviceRegistry.deregister(registration);
            log.info("Deregistered RPC Consul service: name={}, id={}", registration.getServiceId(), registration.getInstanceId());
            registration = null;
            running = false;
        }

        @Override
        public void stop(Runnable callback) {
            try {
                stop();
            } finally {
                callback.run();
            }
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public boolean isAutoStartup() {
            return true;
        }

        @Override
        public int getPhase() {
            return Integer.MAX_VALUE;
        }

        private ConsulRegistration buildRegistration() {
            String serviceName = environment.getProperty(
                    "app.consul.rpc-registration.service-name",
                    environment.getProperty("spring.application.name", "application") + "-rpc"
            );
            int rpcPort = environment.getProperty("grpc.server.port", Integer.class, 9090);
            String instanceId = ConsulAutoRegistration.getInstanceId(discoveryProperties, applicationContext);

            NewService service = new NewService();
            service.setId(ConsulAutoRegistration.normalizeForDns(instanceId + "-rpc"));
            service.setName(ConsulAutoRegistration.normalizeForDns(serviceName));
            service.setPort(rpcPort);
            service.setTags(new ArrayList<>(discoveryProperties.getTags()));
            service.setEnableTagOverride(discoveryProperties.getEnableTagOverride());
            service.setMeta(buildMetadata(rpcPort));
            if (!discoveryProperties.isPreferAgentAddress()) {
                service.setAddress(discoveryProperties.getHostname());
            }
            if (discoveryProperties.isRegisterHealthCheck()) {
                service.setCheck(buildTcpCheck(rpcPort));
            }
            return new ConsulRegistration(service, discoveryProperties);
        }

        private Map<String, String> buildMetadata(int rpcPort) {
            Map<String, String> metadata = new LinkedHashMap<>(discoveryProperties.getMetadata());
            metadata.put("application-name", environment.getProperty("spring.application.name", "application"));
            metadata.put("protocol", "grpc");
            metadata.put("rpc-port", Integer.toString(rpcPort));
            return metadata;
        }

        private NewService.Check buildTcpCheck(int rpcPort) {
            NewService.Check check = new NewService.Check();
            check.setTcp(discoveryProperties.getHostname() + ":" + rpcPort);
            check.setInterval(discoveryProperties.getHealthCheckInterval());
            if (StringUtils.hasText(discoveryProperties.getHealthCheckTimeout())) {
                check.setTimeout(discoveryProperties.getHealthCheckTimeout());
            }
            if (StringUtils.hasText(discoveryProperties.getHealthCheckCriticalTimeout())) {
                check.setDeregisterCriticalServiceAfter(discoveryProperties.getHealthCheckCriticalTimeout());
            }
            return check;
        }
    }
}
