# auth-center

[中文文档](./README.zh-CN.md)

`auth-center` is a layered authentication service built with Spring Boot 3. It provides HTTP and gRPC interfaces for registration, login, logout, password updates, and Consul configuration debugging.

## Tech Stack

- Java 17
- Spring Boot 3.5.12
- Spring Cloud 2025.0.0
- MyBatis Spring Boot Starter 3.0.3
- MySQL 8
- Redis
- Consul Config / Consul Discovery
- gRPC Java 1.72.0
- JJWT 0.11.5

## Modules

- `auth-center-common`: shared annotations, proto files, and common contracts
- `auth-center-domain`: domain models, domain services, and repository ports
- `auth-center-application`: use cases, commands, and application orchestration
- `auth-center-infrastructure`: MyBatis, Redis, JWT, and persistence adapters
- `auth-center-interfaces`: HTTP controllers, request models, and response models
- `auth-center-bootstrap`: Spring Boot entrypoint, runtime wiring, and profile-based configuration

## Architecture Notes

The project uses a DDD-style layered split:

- `domain` owns business rules and repository ports
- `application` coordinates use cases and security context handoff
- `infrastructure` integrates external systems
- `interfaces` adapts HTTP requests and responses
- `bootstrap` assembles runtime configuration

Related design notes are under [`docs/`](./docs), including:

- [`docs/login-architecture.md`](./docs/login-architecture.md)
- [`docs/request-auth-chain.md`](./docs/request-auth-chain.md)
- [`docs/identity-ddd-refactor.md`](./docs/identity-ddd-refactor.md)
- [`docs/service-discovery-consumer-guide.md`](./docs/service-discovery-consumer-guide.md)

## Prerequisites

Before running locally, prepare:

- JDK 17
- Maven 3.9+ or the included `./mvnw`
- Access to GitHub Packages for private dependency download
- For the `dev` profile: Consul, MySQL, and Redis

The project depends on the private package `com.roki:exception-spring-boot-starter:2.0.1`.

Minimal `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

The `server` id must be `github` because the root [`pom.xml`](./pom.xml) uses that repository id.

## Profiles and Configuration

### `dev`

The default local profile is `dev`, defined by Spring Boot startup conventions in [`auth-center-bootstrap/src/main/resources/application-dev.yml`](./auth-center-bootstrap/src/main/resources/application-dev.yml).

This profile:

- enables Consul Config and Consul Discovery
- only requires local Consul connection settings at startup
- resolves MySQL and Redis addresses through service discovery
- reads credentials, JWT, ports, and other runtime configuration from Consul Config
- does not register the current service instance

Default values in the repository:

- `CONSUL_HOST=192.168.31.169`
- `CONSUL_PORT=8500`

The application now reads YAML configuration from the fixed Consul KV path `config/data`.

### `test`

[`auth-center-bootstrap/src/main/resources/application-test.yml`](./auth-center-bootstrap/src/main/resources/application-test.yml) disables:

- Consul config
- Consul service registration and discovery
- backend discovery
- gRPC server
- JDBC/MyBatis auto configuration

This profile is intended for tests, not for a full local runtime.

### `prod`

[`auth-center-bootstrap/src/main/resources/application-prod.yml`](./auth-center-bootstrap/src/main/resources/application-prod.yml) is the production and container deployment profile and keeps Consul integration aligned with `dev`.

This profile:

- enables Consul Config and Consul Discovery
- registers separate HTTP and RPC service instances into Consul
- only requires local Consul connection settings at startup
- resolves MySQL and Redis addresses through service discovery
- reads credentials, JWT, ports, and other runtime configuration from Consul Config

Important environment variables:

- `CONSUL_HOST`
- `CONSUL_PORT`
- `SERVICE_REGISTER_IP` (required; used as the host-reachable registration address published to Consul)
- `APP_HTTP_SERVICE_NAME` (optional; HTTP service name, defaults to `auth-center-http`)
- `APP_RPC_SERVICE_NAME` (optional; RPC service name, defaults to `auth-center-rpc`)

## Run Locally

The application now defaults to the `dev` profile, and you can also specify it explicitly:

```bash
sh ./mvnw -pl auth-center-bootstrap -am clean spring-boot:run
sh ./mvnw -pl auth-center-bootstrap -am clean spring-boot:run -Dspring-boot.run.profiles=dev
```

Start with an explicit profile:

```bash
sh ./mvnw -pl auth-center-bootstrap -am spring-boot:run -Dspring-boot.run.profiles=prod
```

Main class:

- [`auth-center-bootstrap/src/main/java/com/example/authcenter/AuthCenterApplication.java`](./auth-center-bootstrap/src/main/java/com/example/authcenter/AuthCenterApplication.java)

Ports and other runtime settings should now be managed in Consul Config.

For container deployments, the Consul registration address now always uses `SERVICE_REGISTER_IP`. Set it explicitly to the host machine's reachable IP so the instance is not registered with a container-internal or otherwise incorrect address. If it is missing, both `docker compose` and the application startup in the `prod` profile now fail fast with an explicit error.

## Docker Deployment

The repository includes:

- [`docker/Dockerfile`](./docker/Dockerfile)
- [`docker-compose.yml`](./docker-compose.yml)

Build the jar first:

```bash
sh ./mvnw -pl auth-center-bootstrap -am clean package -DskipTests
```

Then start with Docker Compose:

```bash
CONSUL_HOST=your-consul-host \
SERVICE_REGISTER_IP=host-reachable-ip \
docker compose up -d --build
```

Notes:

- `docker-compose.yml` only starts the application container
- MySQL and Redis service registrations should be discoverable in Consul
- credentials, JWT, ports, and other runtime properties should be prepared in Consul Config
- locally, the application only needs to be able to reach Consul
- the container exposes `8080` and `9090`
- Consul registers two distinct services by default: `auth-center-http` for HTTP and `auth-center-rpc` for RPC
- the HTTP service uses `/actuator/health`, while the RPC service uses a TCP health check against `grpc.server.port`

## Service Discovery Contract

Consumers should no longer resolve a single `auth-center` service name and infer ports from metadata. Resolve services by protocol instead:

- discover `auth-center-http` for HTTP calls
- discover `auth-center-rpc` for RPC calls

To customize the published service names, pass them explicitly when starting the provider:

```bash
APP_HTTP_SERVICE_NAME=custom-auth-http \
APP_RPC_SERVICE_NAME=custom-auth-rpc \
CONSUL_HOST=your-consul-host \
SERVICE_REGISTER_IP=host-reachable-ip \
docker compose up -d --build
```

Consumer examples:

- HTTP base URL: `http://auth-center-http`
- gRPC target: `auth-center-rpc:9090` (the actual port should come from the Consul instance record)

For fuller Java consumer examples, see:

- [`docs/service-discovery-consumer-guide.md`](./docs/service-discovery-consumer-guide.md)

## Test

Run all tests:

```bash
sh ./mvnw test
```

Run selected modules:

```bash
sh ./mvnw -pl auth-center-bootstrap -am test
sh ./mvnw -pl auth-center-domain test
```

Representative tests:

- [`auth-center-bootstrap/src/test/java/com/example/authcenter/controller/IdentityAuthFlowTest.java`](./auth-center-bootstrap/src/test/java/com/example/authcenter/controller/IdentityAuthFlowTest.java)
- [`auth-center-bootstrap/src/test/java/com/example/authcenter/identity/usecase/UpdatePasswordUseCaseImplTest.java`](./auth-center-bootstrap/src/test/java/com/example/authcenter/identity/usecase/UpdatePasswordUseCaseImplTest.java)

## HTTP API

Controller entrypoints:

- [`auth-center-interfaces/src/main/java/com/example/authcenter/controller/IdentityController.java`](./auth-center-interfaces/src/main/java/com/example/authcenter/controller/IdentityController.java)
- [`auth-center-interfaces/src/main/java/com/example/authcenter/controller/ConsulConfigDebugController.java`](./auth-center-interfaces/src/main/java/com/example/authcenter/controller/ConsulConfigDebugController.java)

Main endpoints:

### Register

`POST /auth/register`

```json
{
  "username": "tester",
  "password": "123456",
  "email": "tester@example.com"
}
```

### Login

`POST /auth/login`

```json
{
  "username": "tester",
  "password": "123456"
}
```

On success:

- response header includes `Authorization: Bearer <token>`
- response body includes user info and token

### Logout

`POST /auth/logout`

Required header:

```http
Authorization: Bearer <token>
```

### Update Password

`POST /auth/update-password`

```json
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

### Consul Config Debug

`GET /debug/config/consul`

Returns:

- `applicationName`
- `activeProfile`
- `demoMessage`
- `sampleFlag`

## gRPC

Proto definition:

- [`auth-center-common/src/main/proto/auth_center.proto`](./auth-center-common/src/main/proto/auth_center.proto)

Services:

- `authcenter.v1.IdentityService`
- `authcenter.v1.ConfigDebugService`

Exposed identity RPCs:

- `ValidateToken`

Config debug RPC:

- `GetConsulConfig`

## Authentication Flow

- login returns a JWT token
- the HTTP layer validates the token through `JwtInterceptor`
- the current operator is injected through MVC argument resolution

Related components:

- [`auth-center-interfaces/src/main/java/com/example/authcenter/config/JwtInterceptor.java`](./auth-center-interfaces/src/main/java/com/example/authcenter/config/JwtInterceptor.java)
- [`auth-center-interfaces/src/main/java/com/example/authcenter/config/CurrentOperatorArgumentResolver.java`](./auth-center-interfaces/src/main/java/com/example/authcenter/config/CurrentOperatorArgumentResolver.java)

## CI and Repository Rules

Current GitHub Actions workflows:

- `Build & Test Spring Boot App`
- `Commit Message Check`
- `Auto Approve PR`

Commit message rules:

- title format: `type: Chinese title`
- second line must be blank
- body must be a continuous numbered list

Allowed types:

- `build`
- `feat`
- `fix`
- `refactor`
- `test`
- `docs`
- `chore`

Example:

```text
feat: 新增密码修改接口

  1. 新增修改密码请求模型与控制器入口
  2. 接入改密用例并在成功后失效当前会话
  3. 补充密码修改的核心用例测试
```

## Current Caveats

- `dev` and `prod` now both depend on external Consul, with addresses coming from service discovery and runtime properties from Consul Config
- the `prod` profile registers to Consul, so instance registration settings should also be managed through Consul Config
- the repository contains both English and Chinese README files and they should be kept in sync when capabilities change
