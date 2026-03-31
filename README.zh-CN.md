# auth-service

[English](./README.md)

`auth-service` 是一个基于 Spring Boot 3、Spring Cloud Alibaba、MyBatis、Redis 和 MySQL 的认证服务，采用模块化分层结构组织代码，当前主要提供身份认证、会话管理、密码修改以及 Nacos 配置调试能力。

## 技术栈

- Java 17
- Spring Boot 3.5.12
- Spring Cloud 2025.0.0
- Spring Cloud Alibaba 2025.0.0.0
- MyBatis Spring Boot Starter 3.0.3
- MySQL 8
- Redis
- Nacos Config / Nacos Discovery
- JJWT 0.11.5

## 模块结构

- `auth-service-common`
  公共配置、异常定义、注解等通用能力。
- `auth-service-domain`
  领域层，承载 identity 的核心模型、仓储端口、领域服务。
- `auth-service-application`
  应用层，承接 use case、命令对象和应用上下文编排。
- `auth-service-infrastructure`
  基础设施层，提供 MyBatis Mapper、Redis、JWT、仓储实现等技术适配。
- `auth-service-interfaces`
  接口层，提供 HTTP Controller、请求响应模型。
- `auth-service-bootstrap`
  启动层，负责 Spring Boot 应用装配、MVC 配置、拦截器、环境配置。

## 分层约定

项目按 DDD 风格划分职责：

- `domain` 只表达领域模型、领域规则和仓储端口
- `application` 只做用例编排，不承载基础设施细节
- `infrastructure` 实现技术细节和外部系统接入
- `interfaces` 负责协议适配和参数映射
- `bootstrap` 负责运行时装配

## 前置依赖

本地运行前至少需要准备：

- JDK 17
- Maven 3.9+ 或使用仓库自带 `./mvnw`
- MySQL
- Redis
- Nacos

当前 `dev` / `test` 配置默认使用如下地址：

- Nacos: `192.168.31.169:8848`
- MySQL: `192.168.31.169:3306/auth`
- Redis: `192.168.31.169:6379`

如果你的本地环境不同，需要修改：

- [application-dev.yml](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/main/resources/application-dev.yml)
- [application-test.yml](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/main/resources/application-test.yml)

## GitHub Packages 依赖

项目依赖私有包 `com.roki:exception-spring-boot-starter:2.0.1`，来源于 GitHub Packages。

第一次构建前，请确保 Maven 能访问 `github` server。可在 `~/.m2/settings.xml` 中加入：

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

其中 `id` 必须是 `github`，因为根 [pom.xml](/Users/rokilai/IdeaProjects/auth-service/pom.xml) 使用的仓库 id 就是这个名字。

## 配置说明

默认激活环境是 `dev`，定义在：

- [application.yml](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/main/resources/application.yml)

`dev` 环境会：

- 从 Nacos 加载 `auth-service.yml`
- 从 Nacos 加载 `auth-service-dev.yml`
- 注册服务到 Nacos
- 连接 MySQL 和 Redis

`test` 环境会加载：

- `auth-service.yml`
- `auth-service-test.yml`

其中部分 Nacos 参数支持环境变量覆盖：

- `NACOS_SERVER_ADDR`
- `NACOS_USERNAME`
- `NACOS_PASSWORD`
- `NACOS_NAMESPACE`
- `NACOS_GROUP`

## 本地启动

使用 Maven Wrapper：

```bash
sh ./mvnw -pl auth-service-bootstrap clean spring-boot:run
```

如果要显式指定环境：

```bash
sh ./mvnw -pl auth-service-bootstrap spring-boot:run -Dspring-boot.run.profiles=test
```

默认端口：

- `8081`

启动类：

- [AuthServiceApplication.java](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/main/java/com/example/authservice/AuthServiceApplication.java)

## 测试

运行全量测试：

```bash
sh ./mvnw test
```

运行指定模块测试：

```bash
sh ./mvnw -pl auth-service-bootstrap test
sh ./mvnw -pl auth-service-domain test
```

当前仓库里比较关键的测试包括：

- [IdentityAuthFlowTest.java](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/test/java/com/example/authservice/controller/IdentityAuthFlowTest.java)
- [UpdatePasswordUseCaseImplTest.java](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/test/java/com/example/authservice/identity/usecase/UpdatePasswordUseCaseImplTest.java)

## 主要接口

### 1. 注册

`POST /auth/register`

```json
{
  "username": "tester",
  "password": "123456",
  "email": "tester@example.com"
}
```

### 2. 登录

`POST /auth/login`

```json
{
  "username": "tester",
  "password": "123456"
}
```

成功后会：

- 在响应头返回 `Authorization: Bearer <token>`
- 在响应体返回用户基础信息和 token

### 3. 登出

`POST /auth/logout`

请求头需要携带：

```http
Authorization: Bearer <token>
```

### 4. 修改密码

`POST /auth/update-password`

```json
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

### 5. Nacos 配置调试

`GET /debug/config/nacos`

用于查看当前环境下解析到的应用名、激活 profile 和示例配置项。

## 认证流程说明

- 登录成功后，JWT 会通过响应头返回
- 接口层通过 `JwtInterceptor` 校验 token
- 当前操作者会通过 MVC 参数解析注入到应用层命令对象

相关入口可参考：

- [IdentityController.java](/Users/rokilai/IdeaProjects/auth-service/auth-service-interfaces/src/main/java/com/example/authservice/controller/IdentityController.java)
- [JwtInterceptor.java](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/main/java/com/example/authservice/config/JwtInterceptor.java)
- [CurrentOperatorArgumentResolver.java](/Users/rokilai/IdeaProjects/auth-service/auth-service-bootstrap/src/main/java/com/example/authservice/config/CurrentOperatorArgumentResolver.java)

## CI 约定

仓库当前有两条主要 GitHub Actions 工作流：

- `Build & Test Spring Boot App`
- `Commit Message Check`

提交信息规范要求：

- 标题格式：`type: 中文标题`
- 第二行必须为空
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

## 后续可补充内容

如果后面要继续完善 README，建议增加：

- 数据库表结构说明
- 初始化 SQL
- Nacos 配置示例
- Postman / Apifox 调试集合
- 部署方式和生产配置约定

如果要在数据库里清理历史权限表，可直接参考：

- [drop-authorization-tables.sql](/Users/rokilai/IdeaProjects/auth-service/docs/drop-authorization-tables.sql)
