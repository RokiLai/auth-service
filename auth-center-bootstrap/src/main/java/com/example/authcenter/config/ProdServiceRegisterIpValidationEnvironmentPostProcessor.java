package com.example.authcenter.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

public class ProdServiceRegisterIpValidationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }

        String serviceRegisterIp = environment.getProperty("SERVICE_REGISTER_IP");
        if (StringUtils.hasText(serviceRegisterIp)) {
            return;
        }

        throw new IllegalStateException(
                "Missing required environment variable SERVICE_REGISTER_IP for prod profile. " +
                        "Set it to the host machine reachable IP before starting auth-center."
        );
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
