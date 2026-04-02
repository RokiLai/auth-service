# auth-center 服务发现消费指引

本文档说明在 `auth-center` 已拆分 HTTP / RPC 双服务注册后，调用方应如何通过 Consul 正确消费。

## 服务名约定

默认情况下，提供方会注册两个独立服务：

- HTTP 服务：`auth-center-http`
- RPC 服务：`auth-center-rpc`

如果提供方启动时显式传入了 `APP_HTTP_SERVICE_NAME` 或 `APP_RPC_SERVICE_NAME`，则以传入值为准。

调用方不要再查询单个 `auth-center` 服务名并依赖 metadata 推断端口。

## HTTP 调用示例

适用于 Spring Cloud 环境下通过 `DiscoveryClient` 动态解析 HTTP 实例地址。

```java
package com.example.consumer.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AuthCenterHttpClient {

    private static final String HTTP_SERVICE_NAME = "auth-center-http";

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;

    public AuthCenterHttpClient(DiscoveryClient discoveryClient, RestClient.Builder restClientBuilder) {
        this.discoveryClient = discoveryClient;
        this.restClient = restClientBuilder.build();
    }

    public String getConsulDebugConfig() {
        ServiceInstance instance = discoveryClient.getInstances(HTTP_SERVICE_NAME)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No instance found for " + HTTP_SERVICE_NAME));

        String url = instance.getUri() + "/debug/config/consul";
        return restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }
}
```

要点：

- HTTP 调用直接解析 `auth-center-http`
- `instance.getUri()` 会包含 Consul 返回的主机和 HTTP 端口
- 不需要再从 metadata 中读取 RPC 端口

## gRPC 调用示例

适用于 Spring Cloud 环境下通过 `DiscoveryClient` 查询 RPC 服务实例，再建立 `ManagedChannel`。

```java
package com.example.consumer.client;

import com.example.authcenter.grpc.IdentityServiceGrpc;
import com.example.authcenter.grpc.TokenRequest;
import com.example.authcenter.grpc.UserInfoReply;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

@Component
public class AuthCenterRpcClient {

    private static final String RPC_SERVICE_NAME = "auth-center-rpc";

    private final DiscoveryClient discoveryClient;

    public AuthCenterRpcClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public UserInfoReply validateToken(String token) {
        ServiceInstance instance = discoveryClient.getInstances(RPC_SERVICE_NAME)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No instance found for " + RPC_SERVICE_NAME));

        ManagedChannel channel = ManagedChannelBuilder.forAddress(instance.getHost(), instance.getPort())
                .usePlaintext()
                .build();

        try {
            IdentityServiceGrpc.IdentityServiceBlockingStub stub = IdentityServiceGrpc.newBlockingStub(channel);
            return stub.validateToken(TokenRequest.newBuilder().setToken(token).build());
        } finally {
            channel.shutdownNow();
        }
    }
}
```

要点：

- gRPC 调用直接解析 `auth-center-rpc`
- Consul 返回实例端口就是 RPC 端口
- 不需要从 metadata 中解析 `grpc-port` 或 `rpc-port`

## 非 Spring Cloud 调用方

如果调用方不使用 `DiscoveryClient`，原则也一样：

1. 从 Consul 查询 `auth-center-http` 获取 HTTP 实例列表
2. 从 Consul 查询 `auth-center-rpc` 获取 RPC 实例列表
3. 分别按返回实例端口发起 HTTP 或 gRPC 调用

## 迁移建议

如果你的下游还在使用旧模型：

- 旧方式：查 `auth-center` 后从 metadata 推断 HTTP / RPC 端口
- 新方式：HTTP 查 `auth-center-http`，RPC 查 `auth-center-rpc`

建议按以下顺序迁移：

1. 先改消费端服务名
2. 再移除消费端对 `grpc-port`、`rpc-port` metadata 的依赖
3. 最后在所有调用方完成切换后，再考虑是否需要保留兼容字段
