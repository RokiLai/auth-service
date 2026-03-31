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

- runs HTTP on `8081`
- enables Consul Config and Consul Discovery
- registers the service into Consul
- enables backend service discovery for MySQL and Redis
- exposes gRPC on `9090`

Default values in the repository:

- `CONSUL_HOST=192.168.31.169`
- `CONSUL_PORT=8500`
- `HOST_IP=192.168.31.50`
- `MYSQL_SERVICE_NAME=mysql-proxy-service`
- `MYSQL_DATABASE=auth`
- `REDIS_SERVICE_NAME=redis-proxy-service`
- `GRPC_SERVER_PORT=9090`

### `test`

[`auth-center-bootstrap/src/main/resources/application-test.yml`](./auth-center-bootstrap/src/main/resources/application-test.yml) disables:

- Consul config
- Consul service registration and discovery
- backend discovery
- gRPC server
- JDBC/MyBatis auto configuration

This profile is intended for tests, not for a full local runtime.

### `docker`

[`auth-center-bootstrap/src/main/resources/application-docker.yml`](./auth-center-bootstrap/src/main/resources/application-docker.yml) is the standalone deployment profile.

This profile:

- disables Consul config and service registration
- connects directly to MySQL and Redis through environment variables
- runs HTTP on `8080`
- exposes gRPC on `9090`

Important environment variables:

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRE`
- `GRPC_SERVER_PORT`

## Run Locally

Start the application with the default `dev` profile:

```bash
sh ./mvnw -pl auth-center-bootstrap clean spring-boot:run
```

Start with an explicit profile:

```bash
sh ./mvnw -pl auth-center-bootstrap spring-boot:run -Dspring-boot.run.profiles=docker
```

Main class:

- [`auth-center-bootstrap/src/main/java/com/example/authcenter/AuthCenterApplication.java`](./auth-center-bootstrap/src/main/java/com/example/authcenter/AuthCenterApplication.java)

Default ports:

- HTTP: `8081` in `dev`, `8080` in `docker`
- gRPC: `9090`

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
export MYSQL_HOST=your-mysql-host
export MYSQL_USERNAME=your-mysql-user
export MYSQL_PASSWORD=your-mysql-password
export REDIS_HOST=your-redis-host
export JWT_SECRET=your-jwt-secret
docker compose up -d --build
```

Notes:

- `docker-compose.yml` only starts the application container
- MySQL and Redis must already exist outside this compose file
- the container exposes `8080` and `9090`

## Test

Run all tests:

```bash
sh ./mvnw test
```

Run selected modules:

```bash
sh ./mvnw -pl auth-center-bootstrap test
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

- `Register`
- `Login`
- `Logout`
- `UpdatePassword`

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

- `dev` depends on external Consul, MySQL, and Redis services, and the default addresses in the repo are LAN-specific
- the docker deployment profile is the easiest way to run the service without Consul
- the repository contains both English and Chinese README files and they should be kept in sync when capabilities change
