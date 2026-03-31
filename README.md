# auth-center

[中文文档](./README.zh-CN.md)

`auth-center` is an authentication service built with Spring Boot 3, Spring Cloud Alibaba, MyBatis, Redis, and MySQL. The project is organized as a modular layered application and currently provides identity authentication, session management, password updates, and Nacos configuration debugging endpoints.

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

- `auth-center-common`
  Shared annotations, exception definitions, and common configuration.
- `auth-center-domain`
  Domain layer for identity models, repository ports, and domain services.
- `auth-center-application`
  Application layer for use cases, commands, and orchestration logic.
- `auth-center-infrastructure`
  Infrastructure adapters for MyBatis, Redis, JWT, and repository implementations.
- `auth-center-interfaces`
  Interface layer for HTTP controllers and request/response models.
- `auth-center-bootstrap`
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

- [application-dev.yml](/Users/rokilai/IdeaProjects/auth-service/auth-center-bootstrap/src/main/resources/application-dev.yml)
- [application-test.yml](/Users/rokilai/IdeaProjects/auth-service/auth-center-bootstrap/src/main/resources/application-test.yml)

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

- [application.yml](/Users/rokilai/IdeaProjects/auth-service/auth-center-bootstrap/src/main/resources/application.yml)

The `dev` profile:

- imports `auth-center.yml` from Nacos
- imports `auth-center-dev.yml` from Nacos
- registers the service into Nacos
- connects to MySQL and Redis

The `test` profile imports:

- `auth-center.yml`
- `auth-center-test.yml`

Some Nacos properties support environment variable overrides:

- `NACOS_SERVER_ADDR`
- `NACOS_USERNAME`
- `NACOS_PASSWORD`
- `NACOS_NAMESPACE`
- `NACOS_GROUP`

## Run Locally

Start with Maven Wrapper:

```bash
sh ./mvnw -pl auth-center-bootstrap clean spring-boot:run
```

Start with an explicit profile:

```bash
sh ./mvnw -pl auth-center-bootstrap spring-boot:run -Dspring-boot.run.profiles=test
```

Default port:

- `8081`

Main class:

- [AuthCenterApplication.java](/Users/rokilai/IdeaProjects/auth-service/auth-center-bootstrap/src/main/java/com/example/authcenter/AuthCenterApplication.java)

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

- [IdentityAuthFlowTest.java](/Users/rokilai/IdeaProjects/auth-service/auth-center-bootstrap/src/test/java/com/example/authcenter/controller/IdentityAuthFlowTest.java)
- [UpdatePasswordUseCaseImplTest.java](/Users/rokilai/IdeaProjects/auth-service/auth-center-bootstrap/src/test/java/com/example/authcenter/identity/usecase/UpdatePasswordUseCaseImplTest.java)

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

### 5. Nacos Config Debug

`GET /debug/config/nacos`

Returns the current application name, active profile, and a few resolved configuration values for debugging.

## Authentication Flow

- JWT is returned after successful login
- `JwtInterceptor` validates the token at the interface boundary
- the current operator is injected via MVC argument resolution

Related entry points:

- [IdentityController.java](/Users/rokilai/IdeaProjects/auth-service/auth-center-interfaces/src/main/java/com/example/authcenter/controller/IdentityController.java)
- [JwtInterceptor.java](/Users/rokilai/IdeaProjects/auth-service/auth-center-interfaces/src/main/java/com/example/authcenter/config/JwtInterceptor.java)
- [CurrentOperatorArgumentResolver.java](/Users/rokilai/IdeaProjects/auth-service/auth-center-interfaces/src/main/java/com/example/authcenter/config/CurrentOperatorArgumentResolver.java)

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
feat: 新增密码修改接口

  1. 新增修改密码请求模型与控制器入口
  2. 接入改密用例并在成功后失效当前会话
  3. 补充密码修改的核心用例测试
```

## Suggested Future Additions

Useful future extensions for this README:

- database schema overview
- bootstrap SQL
- sample Nacos configuration
- Postman / Apifox collections
- deployment and production configuration notes

For schema cleanup after removing the authorization module, see:

- [drop-authorization-tables.sql](/Users/rokilai/IdeaProjects/auth-service/docs/drop-authorization-tables.sql)
