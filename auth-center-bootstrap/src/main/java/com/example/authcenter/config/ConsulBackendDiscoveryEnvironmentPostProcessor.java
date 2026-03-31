package com.example.authcenter.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConsulBackendDiscoveryEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "consulBackendDiscovery";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("app.backend-discovery.enabled", Boolean.class, false)) {
            return;
        }
        if (!environment.getProperty("spring.cloud.consul.enabled", Boolean.class, true)) {
            return;
        }

        String consulHost = environment.getProperty("spring.cloud.consul.host", "127.0.0.1");
        int consulPort = environment.getProperty("spring.cloud.consul.port", Integer.class, 8500);
        String mysqlServiceName = environment.getProperty("app.backend-discovery.mysql.service-name", "mysql-proxy-service");
        String redisServiceName = environment.getProperty("app.backend-discovery.redis.service-name", "redis-proxy-service");

        ServiceEndpoint mysqlEndpoint = resolveService(consulHost, consulPort, mysqlServiceName);
        ServiceEndpoint redisEndpoint = resolveService(consulHost, consulPort, redisServiceName);

        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("spring.datasource.url", buildMysqlJdbcUrl(environment, mysqlEndpoint));
        overrides.put("spring.data.redis.host", redisEndpoint.host());
        overrides.put("spring.data.redis.port", redisEndpoint.port());
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));

        System.out.printf(
                "Resolved backend services from Consul: mysql=%s:%d, redis=%s:%d%n",
                mysqlEndpoint.host(), mysqlEndpoint.port(), redisEndpoint.host(), redisEndpoint.port()
        );
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    private ServiceEndpoint resolveService(String consulHost, int consulPort, String serviceName) {
        String encodedServiceName = URLEncoder.encode(serviceName, StandardCharsets.UTF_8);
        String url = "http://" + consulHost + ":" + consulPort + "/v1/health/service/" + encodedServiceName + "?passing=true";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Consul returned HTTP " + response.statusCode() + " for service " + serviceName);
            }

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            if (!root.isArray() || root.isEmpty()) {
                throw new IllegalStateException("No passing instance found in Consul for service " + serviceName);
            }

            JsonNode first = root.get(0);
            JsonNode serviceNode = first.path("Service");
            JsonNode nodeNode = first.path("Node");

            String host = textOrNull(serviceNode.path("Address"));
            if (!StringUtils.hasText(host)) {
                host = textOrNull(nodeNode.path("Address"));
            }
            if (!StringUtils.hasText(host)) {
                throw new IllegalStateException("Resolved empty host from Consul for service " + serviceName);
            }

            int port = serviceNode.path("Port").asInt();
            if (port <= 0) {
                throw new IllegalStateException("Resolved invalid port from Consul for service " + serviceName);
            }

            return new ServiceEndpoint(host, port);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse Consul response for service " + serviceName, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while resolving service " + serviceName + " from Consul", ex);
        }
    }

    private String buildMysqlJdbcUrl(ConfigurableEnvironment environment, ServiceEndpoint endpoint) {
        String database = environment.getProperty("app.backend-discovery.mysql.database", "auth");
        String params = environment.getProperty(
                "app.backend-discovery.mysql.jdbc-params",
                "useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false"
        );
        return "jdbc:mysql://" + endpoint.host() + ":" + endpoint.port() + "/" + database + "?" + params;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return StringUtils.hasText(value) ? value : null;
    }

    private record ServiceEndpoint(String host, int port) {
    }
}
