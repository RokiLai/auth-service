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

- 使用 `8081` 暴露 HTTP 服务
- 开启 Consul Config 和 Consul Discovery
- 将服务注册到 Consul
- 开启 MySQL 和 Redis 的后端服务发现
- 使用 `9090` 暴露 gRPC 服务

仓库中的默认值包括：

- `CONSUL_HOST=192.168.31.169`
- `CONSUL_PORT=8500`
- `HOST_IP=192.168.31.50`
- `MYSQL_SERVICE_NAME=mysql-proxy-service`
- `MYSQL_DATABASE=auth`
- `REDIS_SERVICE_NAME=redis-proxy-service`
- `GRPC_SERVER_PORT=9090`

### `test`

[`auth-center-bootstrap/src/main/resources/application-test.yml`](./auth-center-bootstrap/src/main/resources/application-test.yml) 会关闭：

- Consul 配置
- Consul 服务注册与发现
- 后端服务发现
- gRPC 服务
- JDBC / MyBatis 自动配置

这个 profile 主要用于测试，不适合作为完整的本地运行环境。

### `docker`

[`auth-center-bootstrap/src/main/resources/application-docker.yml`](./auth-center-bootstrap/src/main/resources/application-docker.yml) 用于容器化部署，并保持与 `dev` 接近的 Consul 接入方式。

该 profile 会：

- 开启 Consul Config 和 Consul Discovery
- 将容器实例注册到 Consul
- 优先通过 Consul 发现 MySQL 和 Redis
- 同时保留环境变量形式的 MySQL / Redis 直连配置作为兜底
- 使用 `8080` 暴露 HTTP 服务
- 使用 `9090` 暴露 gRPC 服务

关键环境变量：

- `CONSUL_HOST`
- `CONSUL_PORT`
- `HOST_IP`
- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `MYSQL_SERVICE_NAME`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `REDIS_SERVICE_NAME`
- `JWT_SECRET`
- `JWT_EXPIRE`
- `GRPC_SERVER_PORT`

## 本地启动

使用默认 `dev` profile 启动：

```bash
sh ./mvnw -pl auth-center-bootstrap clean spring-boot:run
```

显式指定 profile 启动：

```bash
sh ./mvnw -pl auth-center-bootstrap spring-boot:run -Dspring-boot.run.profiles=docker
```

启动类：

- [`auth-center-bootstrap/src/main/java/com/example/authcenter/AuthCenterApplication.java`](./auth-center-bootstrap/src/main/java/com/example/authcenter/AuthCenterApplication.java)

默认端口：

- HTTP：`dev` 为 `8081`，`docker` 为 `8080`
- gRPC：`9090`

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
export MYSQL_HOST=你的数据库地址
export MYSQL_USERNAME=你的数据库用户名
export MYSQL_PASSWORD=你的数据库密码
export REDIS_HOST=你的 Redis 地址
export JWT_SECRET=你的 JWT 密钥
docker compose up -d --build
```

说明：

- `docker-compose.yml` 只启动应用容器本身
- MySQL 和 Redis 需要在 Compose 外部提前准备好
- `CONSUL_HOST` 和 `HOST_IP` 必须显式提供；缺失时应直接暴露配置错误
- `HOST_IP` 必须配置成 Consul 可访问到的容器宿主机或容器出口地址
- 当前容器暴露端口为 `8080` 和 `9090`

## 测试

运行全量测试：

```bash
sh ./mvnw test
```

运行指定模块测试：

```bash
sh ./mvnw -pl auth-center-bootstrap test
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

- `Register`
- `Login`
- `Logout`
- `UpdatePassword`

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

- `dev` 环境依赖外部 Consul、MySQL、Redis，仓库内默认地址更偏向局域网开发环境
- `docker` profile 现在也会接入并注册到 Consul，部署时需要显式提供 `CONSUL_HOST` 和可被 Consul 回连的 `HOST_IP`
- 仓库同时维护中英文两份 README，后续功能变更时需要同步更新
