# auth-center

[English](./README.md)

`auth-center` 是一个基于 Spring Boot 3 的认证中心服务，采用分层模块化结构，当前同时提供 HTTP 和 gRPC 两套接口，覆盖注册、登录、登出、修改密码以及 Consul 配置调试能力。

## 技术栈

- Java 17
- Spring Boot 3.5.12
- Spring Cloud 2025.0.0
- MyBatis Spring Boot Starter 3.0.3
- MySQL 8
- Redis
- Consul Config / Consul Discovery
- gRPC Java 1.72.0
- JJWT 0.11.5

## 模块结构

- `auth-center-common`：公共注解、proto 定义和共享契约
- `auth-center-domain`：领域模型、领域服务、仓储端口
- `auth-center-application`：用例编排、命令对象、应用层上下文
- `auth-center-infrastructure`：MyBatis、Redis、JWT、持久化适配
- `auth-center-interfaces`：HTTP 控制器、请求模型、响应模型
- `auth-center-bootstrap`：Spring Boot 启动入口、运行时装配、按 profile 切换配置

## 架构说明

项目采用 DDD 风格的分层职责拆分：

- `domain` 负责业务规则和仓储端口
- `application` 负责编排用例和认证上下文传递
- `infrastructure` 负责外部系统接入
- `interfaces` 负责 HTTP 协议适配
- `bootstrap` 负责运行时装配和配置加载

相关设计文档位于 [`docs/`](./docs)：

- [`docs/login-architecture.md`](./docs/login-architecture.md)
- [`docs/request-auth-chain.md`](./docs/request-auth-chain.md)
- [`docs/identity-ddd-refactor.md`](./docs/identity-ddd-refactor.md)
- [`docs/service-discovery-consumer-guide.md`](./docs/service-discovery-consumer-guide.md)

## 运行前准备

本地运行前需要准备：

- JDK 17
- Maven 3.9+，或直接使用仓库内的 `./mvnw`
- 可访问 GitHub Packages 的认证信息
- 在 `dev` 环境下可用的 Consul、MySQL、Redis

项目依赖 GitHub Packages 上的私有包 `com.roki:exception-spring-boot-starter:2.0.1`。

最小可用的 `~/.m2/settings.xml` 示例：

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

其中 `server.id` 必须是 `github`，因为根 [`pom.xml`](./pom.xml) 中的仓库 id 就是这个名字。

## Profile 与配置

### `dev`

本地默认运行方式是 `dev`，主要配置在 [`auth-center-bootstrap/src/main/resources/application-dev.yml`](./auth-center-bootstrap/src/main/resources/application-dev.yml)。

该 profile 会：

- 开启 Consul Config 和 Consul Discovery
- 启动时只要求本地提供 Consul 连接信息
- MySQL 和 Redis 地址通过服务发现获取
- 账号密码、JWT、端口等业务配置从 Consul Config 读取
- 不注册当前服务实例

仓库中的默认值包括：

- `CONSUL_HOST=192.168.31.169`
- `CONSUL_PORT=8500`

当前会固定从 Consul KV 路径 `config/data` 读取 YAML 配置。

### `test`

[`auth-center-bootstrap/src/main/resources/application-test.yml`](./auth-center-bootstrap/src/main/resources/application-test.yml) 会关闭：

- Consul 配置
- Consul 服务注册与发现
- 后端服务发现
- gRPC 服务
- JDBC / MyBatis 自动配置

这个 profile 主要用于测试，不适合作为完整的本地运行环境。

### `prod`

[`auth-center-bootstrap/src/main/resources/application-prod.yml`](./auth-center-bootstrap/src/main/resources/application-prod.yml) 用于生产或容器化部署，并保持与 `dev` 接近的 Consul 接入方式。

该 profile 会：

- 开启 Consul Config 和 Consul Discovery
- 将 HTTP 和 RPC 两个服务实例分别注册到 Consul
- 启动时只要求本地提供 Consul 连接信息
- MySQL 和 Redis 地址通过服务发现获取
- 账号密码、JWT、端口等业务配置从 Consul Config 读取

关键环境变量：

- `CONSUL_HOST`
- `CONSUL_PORT`
- `SERVICE_REGISTER_IP`（必填，用于显式指定注册到 Consul 的宿主机地址）
- `APP_HTTP_SERVICE_NAME`（可选，HTTP 服务名，默认 `auth-center-http`）
- `APP_RPC_SERVICE_NAME`（可选，RPC 服务名，默认 `auth-center-rpc`）

## 本地启动

默认会以 `dev` profile 启动，也可以显式指定：

```bash
sh ./mvnw -pl auth-center-bootstrap -am clean spring-boot:run
sh ./mvnw -pl auth-center-bootstrap -am clean spring-boot:run -Dspring-boot.run.profiles=dev
```

显式指定 profile 启动：

```bash
sh ./mvnw -pl auth-center-bootstrap -am spring-boot:run -Dspring-boot.run.profiles=prod
```

启动类：

- [`auth-center-bootstrap/src/main/java/com/example/authcenter/AuthCenterApplication.java`](./auth-center-bootstrap/src/main/java/com/example/authcenter/AuthCenterApplication.java)

服务端口等运行参数建议统一维护在 Consul Config 中。

容器部署时，Consul 注册地址固定使用 `SERVICE_REGISTER_IP`。请显式传入宿主机实际可访问 IP，避免注册为容器内网地址或错误网卡地址；未提供时，`docker compose` 和应用 `prod` 启动都会直接失败并给出明确提示。

## Docker 部署

仓库内已提供：

- [`docker/Dockerfile`](./docker/Dockerfile)
- [`docker-compose.yml`](./docker-compose.yml)

先构建可执行 jar：

```bash
sh ./mvnw -pl auth-center-bootstrap -am clean package -DskipTests
```

再通过 Docker Compose 启动：

```bash
CONSUL_HOST=你的 Consul 地址 \
SERVICE_REGISTER_IP=宿主机实际IP \
docker compose up -d --build
```

说明：

- `docker-compose.yml` 只启动应用容器本身
- MySQL、Redis 的服务注册地址应在 Consul 中可发现
- 账号密码、JWT、端口等业务配置应在 Consul Config 中提前准备好
- 本地只需要保证应用能连到 Consul
- 当前容器暴露端口为 `8080` 和 `9090`
- Consul 中会注册两个独立服务：HTTP 服务默认为 `auth-center-http`，RPC 服务默认为 `auth-center-rpc`
- HTTP 服务使用 `/actuator/health` 做健康检查，RPC 服务使用 TCP 检查 `grpc.server.port`

## 服务发现约定

调用方不应再通过单个 `auth-center` 服务名加 metadata 推断端口，而应直接按协议区分服务名：

- HTTP 调用发现 `auth-center-http`
- RPC 调用发现 `auth-center-rpc`

如需自定义服务名，可在提供方启动时显式传入：

```bash
APP_HTTP_SERVICE_NAME=custom-auth-http \
APP_RPC_SERVICE_NAME=custom-auth-rpc \
CONSUL_HOST=你的 Consul 地址 \
SERVICE_REGISTER_IP=宿主机实际IP \
docker compose up -d --build
```

消费端示例：

- HTTP 基地址：`http://auth-center-http`
- gRPC 目标：`auth-center-rpc:9090`（实际端口以 Consul 返回实例端口为准）

更完整的 Java 消费端接入示例见：

- [`docs/service-discovery-consumer-guide.md`](./docs/service-discovery-consumer-guide.md)

## 测试

运行全量测试：

```bash
sh ./mvnw test
```

运行指定模块测试：

```bash
sh ./mvnw -pl auth-center-bootstrap -am test
sh ./mvnw -pl auth-center-domain test
```

代表性测试：

- [`auth-center-bootstrap/src/test/java/com/example/authcenter/controller/IdentityAuthFlowTest.java`](./auth-center-bootstrap/src/test/java/com/example/authcenter/controller/IdentityAuthFlowTest.java)
- [`auth-center-bootstrap/src/test/java/com/example/authcenter/identity/usecase/UpdatePasswordUseCaseImplTest.java`](./auth-center-bootstrap/src/test/java/com/example/authcenter/identity/usecase/UpdatePasswordUseCaseImplTest.java)

## HTTP 接口

控制器入口：

- [`auth-center-interfaces/src/main/java/com/example/authcenter/controller/IdentityController.java`](./auth-center-interfaces/src/main/java/com/example/authcenter/controller/IdentityController.java)
- [`auth-center-interfaces/src/main/java/com/example/authcenter/controller/ConsulConfigDebugController.java`](./auth-center-interfaces/src/main/java/com/example/authcenter/controller/ConsulConfigDebugController.java)

主要接口如下。

### 注册

`POST /auth/register`

```json
{
  "username": "tester",
  "password": "123456",
  "email": "tester@example.com"
}
```

### 登录

`POST /auth/login`

```json
{
  "username": "tester",
  "password": "123456"
}
```

成功后会：

- 在响应头返回 `Authorization: Bearer <token>`
- 在响应体返回用户信息和 token

### 登出

`POST /auth/logout`

请求头需要携带：

```http
Authorization: Bearer <token>
```

### 修改密码

`POST /auth/update-password`

```json
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

### Consul 配置调试

`GET /debug/config/consul`

返回字段包括：

- `applicationName`
- `activeProfile`
- `demoMessage`
- `sampleFlag`

## gRPC

proto 定义：

- [`auth-center-common/src/main/proto/auth_center.proto`](./auth-center-common/src/main/proto/auth_center.proto)

对外服务：

- `authcenter.v1.IdentityService`
- `authcenter.v1.ConfigDebugService`

身份相关 RPC：

- `ValidateToken`

配置调试 RPC：

- `GetConsulConfig`

## 认证链路

- 登录成功后返回 JWT
- HTTP 接口层通过 `JwtInterceptor` 校验 token
- 当前操作者通过 MVC 参数解析器注入到业务用例

相关组件：

- [`auth-center-interfaces/src/main/java/com/example/authcenter/config/JwtInterceptor.java`](./auth-center-interfaces/src/main/java/com/example/authcenter/config/JwtInterceptor.java)
- [`auth-center-interfaces/src/main/java/com/example/authcenter/config/CurrentOperatorArgumentResolver.java`](./auth-center-interfaces/src/main/java/com/example/authcenter/config/CurrentOperatorArgumentResolver.java)

## CI 与仓库约定

当前主要 GitHub Actions 工作流：

- `Build & Test Spring Boot App`
- `Commit Message Check`
- `Auto Approve PR`

提交信息规范要求：

- 标题格式：`type: 中文标题`
- 第二行必须为空行
- 正文必须是连续编号列表

允许的 `type`：

- `build`
- `feat`
- `fix`
- `refactor`
- `test`
- `docs`
- `chore`

示例：

```text
feat: 新增密码修改接口

  1. 新增修改密码请求模型与控制器入口
  2. 接入改密用例并在成功后失效当前会话
  3. 补充密码修改的核心用例测试
```

## 当前注意点

- `dev` 和 `prod` 现在都依赖外部 Consul，地址走服务发现，业务配置走 Consul Config
- `prod` profile 会注册到 Consul，因此与实例注册地址相关的配置也应由 Consul 配置中心统一管理
- 仓库同时维护中英文两份 README，后续功能变更时需要同步更新
