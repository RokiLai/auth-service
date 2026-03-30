# auth-service

[中文文档](./README.zh-CN.md)

`auth-service` is an authentication and authorization service built with Spring Boot 3, Spring Cloud Alibaba, MyBatis, Redis, and MySQL. The project is organized as a modular layered application and currently provides identity authentication, session management, password updates, role authorization, and Nacos configuration debugging endpoints.

## Tech Stack

- Java 17
- Spring Boot 3.5.12
- Spring Cloud 2025.0.0
- Spring Cloud Alibaba 2025.0.0.0
- MyBatis Spring Boot Starter 3.0.3
- MySQL 8
- Redis
- Nacos Config / Nacos Discovery
- JJWT 0.11.5

## Modules

- `auth-service-common`
  Shared annotations, exception definitions, and common configuration.
- `auth-service-domain`
  Domain layer for identity and authorization models, repository ports, and domain services.
- `auth-service-application`
  Application layer for use cases, commands, and orchestration logic.
- `auth-service-infrastructure`
  Infrastructure adapters for MyBatis, Redis, JWT, and repository implementations.
- `auth-service-interfaces`
  Interface layer for HTTP controllers and request/response models.
- `auth-service-bootstrap`
  Runtime bootstrap layer for Spring Boot startup, MVC config, interceptors, and environment wiring.

## Layering Rules

This project follows a DDD-style module split:

- `domain` expresses domain models, rules, and repository ports
- `application` orchestrates use cases without owning infrastructure details
- `infrastructure` implements technical integration and external systems
- `interfaces` handles transport mapping and protocol adaptation
- `bootstrap` assembles the runtime application

## Prerequisites

Before running locally, prepare:

- JDK 17
- Maven 3.9+ or the included `./mvnw`
- MySQL
- Redis
- Nacos

Current `dev` and `test` profiles default to:

- Nacos: `192.168.31.169:8848`
- MySQL: `192.168.31.169:3306/auth`
- Redis: `192.168.31.169:6379`

If your local environment is different, update:

- [application-dev.yml](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/main/resources/application-dev.yml)
- [application-test.yml](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/main/resources/application-test.yml)

## GitHub Packages Dependency

The project depends on the private package `com.roki:exception-spring-boot-starter:2.0.1` hosted on GitHub Packages.

Before the first build, make sure Maven can authenticate to the `github` server. A minimal `~/.m2/settings.xml` example:

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

The `server` id must be `github`, because the root [pom.xml](/Users/rokilai/IdeaProjects/auth-service/pom.xml) uses that repository id.

## Configuration

The default active profile is `dev`, defined in:

- [application.yml](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/main/resources/application.yml)

The `dev` profile:

- imports `auth-service.yml` from Nacos
- imports `auth-service-dev.yml` from Nacos
- registers the service into Nacos
- connects to MySQL and Redis

The `test` profile imports:

- `auth-service.yml`
- `auth-service-test.yml`

Some Nacos properties support environment variable overrides:

- `NACOS_SERVER_ADDR`
- `NACOS_USERNAME`
- `NACOS_PASSWORD`
- `NACOS_NAMESPACE`
- `NACOS_GROUP`

## Run Locally

Start with Maven Wrapper:

```bash
sh ./mvnw -pl auth-service-bootstrap clean spring-boot:run
```

Start with an explicit profile:

```bash
sh ./mvnw -pl auth-service-bootstrap spring-boot:run -Dspring-boot.run.profiles=test
```

Default port:

- `8081`

Main class:

- [AuthServiceApplication.java](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/main/java/com/example/authservice/AuthServiceApplication.java)

## Test

Run all tests:

```bash
sh ./mvnw test
```

Run selected modules:

```bash
sh ./mvnw -pl auth-service-bootstrap test
sh ./mvnw -pl auth-service-domain test
```

Representative tests:

- [IdentityAuthFlowTest.java](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/test/java/com/example/authservice/controller/IdentityAuthFlowTest.java)
- [AuthorizationControllerTest.java](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/test/java/com/example/authservice/controller/AuthorizationControllerTest.java)
- [AuthorizationDomainServiceImplTest.java](/Users/rokilai/IdeaProjects/auth-service/auth-service-domain/src/test/java/com/example/authservice/domain/authorization/service/impl/AuthorizationDomainServiceImplTest.java)

## Main Endpoints

### 1. Register

`POST /auth/register`

```json
{
  "username": "tester",
  "password": "123456",
  "email": "tester@example.com"
}
```

### 2. Login

`POST /auth/login`

```json
{
  "username": "tester",
  "password": "123456"
}
```

On success:

- response header contains `Authorization: Bearer <token>`
- response body contains user info and token

### 3. Logout

`POST /auth/logout`

Required header:

```http
Authorization: Bearer <token>
```

### 4. Update Password

`POST /auth/update-password`

```json
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

### 5. Authorize Role

`POST /authorization/roles/authorize`

```json
{
  "roleId": 1,
  "permissionIds": [2, 3]
}
```

### 6. Nacos Config Debug

`GET /debug/config/nacos`

Returns the current application name, active profile, and a few resolved configuration values for debugging.

## Authentication and Authorization Flow

- JWT is returned after successful login
- `JwtInterceptor` validates the token at the interface boundary
- the current operator is injected via MVC argument resolution
- role authorization is handled by the authorization domain service
- session role/permission snapshots are assembled inside the authentication domain service

Related entry points:

- [IdentityController.java](/Users/rokilai/IdeaProjects/auth-service/auth-service-interfaces/src/main/java/com/example/authservice/controller/IdentityController.java)
- [AuthorizationController.java](/Users/rokilai/IdeaProjects/auth-service/auth-service-interfaces/src/main/java/com/example/authservice/controller/AuthorizationController.java)
- [JwtInterceptor.java](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/main/java/com/example/authservice/config/JwtInterceptor.java)
- [CurrentOperatorArgumentResolver.java](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/main/java/com/example/authservice/config/CurrentOperatorArgumentResolver.java)

## CI Conventions

The repository currently has two main GitHub Actions workflows:

- `Build & Test Spring Boot App`
- `Commit Message Check`

Commit message rules:

- title format: `type: Chinese title`
- second line must be blank
- body must be a continuous numbered list

Allowed `type` values:

- `build`
- `feat`
- `fix`
- `refactor`
- `test`
- `docs`
- `chore`

Example:

```text
feat: 新增角色授权接口

  1. 新增角色授权控制器与请求模型
  2. 引入授权命令对象并调整应用层调用
  3. 补充授权接口鉴权与参数校验测试
```

## Suggested Future Additions

Useful future extensions for this README:

- database schema overview
- bootstrap SQL
- sample Nacos configuration
- Postman / Apifox collections
- deployment and production configuration notes
