# 请求鉴权拦截链路

## 目标

描述一个带 `Authorization: Bearer <token>` 的请求，从进入服务到到达 controller 层之前，当前项目是如何完成鉴权和身份注入的。

## 总览流程图

```mermaid
flowchart TD
    A["客户端请求<br/>Authorization: Bearer token"] --> B["DispatcherServlet.doDispatch(...)"]
    B --> C["JwtInterceptor.preHandle(request, response, handler)"]

    C --> D{"handler 是否为 HandlerMethod"}
    D -- 否 --> Z["直接放行"]
    D -- 是 --> E["handlerMethod.getMethod()"]
    E --> F{"method 是否标注 @PassToken"}

    F -- 是 --> G["passToken.required()"]
    G -- true --> Z
    F -- 否 --> H["request.getHeader('Authorization')"]
    H --> I["JwtInterceptor.resolveToken(authorizationHeader)"]
    I --> J["AuthenticateUseCase.authenticate(rawToken)"]

    J --> K["IdentityTokenProvider.parse(rawToken)"]
    K --> L["TokenClaims.getSessionId()"]
    L --> M["IdentitySessionRepository.findBySessionId(sessionId)"]
    M --> N{"session 存在<br/>且 token 匹配"}

    N -- 否 --> O["抛出 TokenInvalidException<br/>或 TokenExpiredException"]
    N -- 是 --> P["组装 CurrentOperator"]
    P --> Q["request.setAttribute('currentOperator', currentOperator)"]

    Q --> R["InvocableHandlerMethod.getMethodArgumentValues(...)"]
    R --> S["CurrentOperatorArgumentResolver.supportsParameter(...)"]
    S --> T["CurrentOperatorArgumentResolver.resolveArgument(...)"]
    T --> U["request.getAttribute('currentOperator')"]
    U --> V["Controller 方法执行"]
```

## 时序图

```mermaid
sequenceDiagram
    participant Client as 客户端
    participant MVC as DispatcherServlet
    participant Interceptor as JwtInterceptor
    participant UseCase as AuthenticateUseCaseImpl
    participant TokenProvider as IdentityTokenProvider
    participant SessionRepo as IdentitySessionRepository
    participant Resolver as CurrentOperatorArgumentResolver
    participant Controller as Controller

    Client->>MVC: doDispatch(request, response)
    MVC->>Interceptor: preHandle(request, response, handler)

    alt 不是 Controller 方法
        Interceptor-->>MVC: true
    else 是 Controller 方法
        Interceptor->>Interceptor: handlerMethod.getMethod()
        Interceptor->>Interceptor: method.isAnnotationPresent(PassToken.class)
        alt 标注了 @PassToken
            Interceptor-->>MVC: true
        else 需要鉴权
            Interceptor->>Interceptor: request.getHeader("Authorization")
            Interceptor->>Interceptor: resolveToken(authorizationHeader)
            Interceptor->>UseCase: authenticate(rawToken)
            UseCase->>TokenProvider: parse(rawToken)
            TokenProvider-->>UseCase: TokenClaims
            UseCase->>UseCase: claims.getSessionId()
            UseCase->>SessionRepo: findBySessionId(sessionId)
            SessionRepo-->>UseCase: IdentitySession
            UseCase->>UseCase: session.matchesToken(rawToken)
            UseCase->>UseCase: new CurrentOperator(...)
            UseCase-->>Interceptor: CurrentOperator
            Interceptor->>Interceptor: request.setAttribute("currentOperator", currentOperator)
            Interceptor-->>MVC: true
        end
    end

    MVC->>Resolver: supportsParameter(parameter)
    Resolver-->>MVC: true
    MVC->>Resolver: resolveArgument(parameter, mavContainer, webRequest, binderFactory)
    Resolver->>Resolver: webRequest.getNativeRequest(HttpServletRequest.class)
    Resolver->>Resolver: request.getAttribute("currentIdentity")
    Resolver-->>MVC: CurrentOperator
    MVC->>Controller: logout(currentOperator) / updatePassword(request, currentOperator)
```

## 方法调用清单

按当前代码实现，请求到 controller 前的关键方法调用顺序如下：

```text
DispatcherServlet.doDispatch(...)
  -> HandlerExecutionChain.applyPreHandle(...)
    -> JwtInterceptor.preHandle(request, response, handler)
      -> handlerMethod.getMethod()
      -> method.isAnnotationPresent(PassToken.class)
      -> request.getHeader("Authorization")
      -> JwtInterceptor.resolveToken(authorizationHeader)
      -> AuthenticateUseCaseImpl.authenticate(rawToken)
        -> IdentityTokenProvider.parse(rawToken)
        -> TokenClaims.getSessionId()
        -> IdentitySessionRepository.findBySessionId(sessionId)
        -> IdentitySession.matchesToken(rawToken)
        -> new CurrentOperator(...)
      -> request.setAttribute("currentOperator", currentOperator)
  -> InvocableHandlerMethod.getMethodArgumentValues(...)
    -> CurrentOperatorArgumentResolver.supportsParameter(parameter)
    -> CurrentOperatorArgumentResolver.resolveArgument(...)
      -> webRequest.getNativeRequest(HttpServletRequest.class)
      -> request.getAttribute("currentOperator")
  -> Controller.logout(@AuthIdentity CurrentOperator)
     或 Controller.updatePassword(..., @AuthIdentity CurrentOperator)
```

## Controller 层看到的形态

鉴权成功后，controller 不需要自己读取 `HttpServletRequest`，而是直接声明参数：

```java
@PostMapping("/logout")
public Result<Boolean> logout(@AuthIdentity CurrentOperator currentOperator) {
    return Result.success(logoutUseCase.logout(new LogoutCommand(currentOperator)));
}
```

```java
@PostMapping("/update-password")
public Result<Boolean> updatePassword(@Valid @RequestBody UpdatePasswordRequest request,
                                      @AuthIdentity CurrentOperator currentOperator) {
    updatePasswordUseCase.updatePassword(new UpdatePasswordCommand(
            currentOperator,
            request.getOldPassword(),
            request.getNewPassword()
    ));
    return Result.success(true);
}
```

## 当前项目代码映射

- 拦截入口
  - `auth-center-bootstrap/src/main/java/com/example/authcenter/config/JwtInterceptor.java`
- 鉴权用例
  - `auth-center-application/src/main/java/com/example/authcenter/identity/usecase/impl/AuthenticateUseCaseImpl.java`
- 会话仓储
  - `auth-center-domain/src/main/java/com/example/authcenter/domain/identity/repository/IdentitySessionRepository.java`
  - `auth-center-infrastructure/src/main/java/com/example/authcenter/infra/service/RedisSessionStoreImpl.java`
- 当前身份参数解析器
  - `auth-center-interfaces/src/main/java/com/example/authcenter/config/CurrentOperatorArgumentResolver.java`
- MVC 注册
  - `auth-center-bootstrap/src/main/java/com/example/authcenter/config/WebConfig.java`
- controller 身份注入点
  - `auth-center-interfaces/src/main/java/com/example/authcenter/controller/IdentityController.java`

## 分层职责

- `bootstrap`
  - 负责拦截请求、调用鉴权用例、把身份写入 request、注册参数解析器
- `application`
  - 负责 token 鉴权用例编排
- `domain`
  - 负责会话仓储端口、token 解析端口、登录态有效性规则
- `infrastructure`
  - 负责 Redis 会话存储、JWT 解析实现
- `interfaces`
  - 负责接收已认证身份并继续调用应用层用例

## 这条链路的设计要点

- 身份上下文只在接口适配层传递，不再依赖 `ThreadLocal`
- `JwtInterceptor` 只负责“认证并写入 request attribute”
- controller 通过 `@AuthIdentity` 显式声明自己依赖当前身份
- application 层只接收显式参数，不感知 HTTP 请求对象
