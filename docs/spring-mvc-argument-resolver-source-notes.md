# Spring MVC 参数解析源码阅读笔记

## 目标

结合当前项目的 `@AuthIdentity CurrentOperator` 参数注入场景，梳理 Spring MVC 在源码层面是如何完成以下动作的：

- 初始化参数解析器链
- 选择某个参数对应的 resolver
- 按顺序调用 `supportsParameter(...)` 和 `resolveArgument(...)`
- 最终调用 controller 方法

本文基于当前项目实际依赖的 Spring 版本：

- `Spring Boot 3.5.12`
- `spring-webmvc 6.2.17`
- `spring-web 6.2.17`

## 先记住主链路

```text
RequestMappingHandlerAdapter
  -> invokeHandlerMethod(...)
  -> ServletInvocableHandlerMethod / InvocableHandlerMethod
  -> getMethodArgumentValues(...)
  -> HandlerMethodArgumentResolverComposite
  -> supportsParameter(...)
  -> resolveArgument(...)
  -> doInvoke(...)
  -> Controller method
```

如果你只想抓主干，就围绕这条链往下读。

## 一、RequestMappingHandlerAdapter

这个类是 `@RequestMapping` 方法的总调度器。

### 1. 初始化 resolver 链

在 `afterPropertiesSet()` 里：

```java
if (this.argumentResolvers == null) {
    List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();
    this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
}
```

这段说明：

- Spring 启动时会先准备完整的参数解析器列表
- 再把它们装进 `HandlerMethodArgumentResolverComposite`

也就是说，后续 controller 参数解析时，面对的不是单个 resolver，而是一整个 resolver 链。

### 2. 内置 resolver 和自定义 resolver 的顺序

在 `getDefaultArgumentResolvers()` 里：

```java
// Annotation-based argument resolution
resolvers.add(new RequestParamMethodArgumentResolver(...));
resolvers.add(new RequestResponseBodyMethodProcessor(...));
...

// Type-based argument resolution
resolvers.add(new ServletRequestMethodArgumentResolver());
...

// Custom arguments
if (getCustomArgumentResolvers() != null) {
    resolvers.addAll(getCustomArgumentResolvers());
}

// Catch-all
resolvers.add(new PrincipalMethodArgumentResolver());
resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), true));
resolvers.add(new ServletModelAttributeMethodProcessor(true));
```

这里能看出 3 个规则：

1. Spring 先注册一批内置 resolver
2. 你通过 `WebMvcConfigurer#addArgumentResolvers(...)` 加进去的 resolver，会被放进 `customArgumentResolvers`
3. custom resolver 在大多数内置 resolver 后面、在 catch-all resolver 前面

对你这个项目来说，`CurrentOperatorArgumentResolver` 就是按这个规则进链的。

### 3. 真正开始调用 controller 前

在 `invokeHandlerMethod(...)` 里：

```java
ServletInvocableHandlerMethod invocableMethod = createInvocableHandlerMethod(handlerMethod);
if (this.argumentResolvers != null) {
    invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
}
...
invocableMethod.invokeAndHandle(webRequest, mavContainer);
```

这说明：

- Spring 先把目标 controller 方法包装成 `InvocableHandlerMethod`
- 再把 resolver 链塞进去
- 然后把“参数解析 + 方法调用”的执行权交给 `InvocableHandlerMethod`

所以真正驱动参数解析的，不是 `RequestMappingHandlerAdapter` 本身，而是 `InvocableHandlerMethod`。

## 二、InvocableHandlerMethod

这个类是参数解析和方法调用的核心执行者。

### 1. 入口方法

在 `invokeForRequest(...)` 里：

```java
Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
Object returnValue = doInvoke(args);
```

含义很直白：

1. 先把 controller 方法参数全部解析出来
2. 再真正调用方法

所以参数解析一定发生在 controller 方法执行之前。

### 2. 参数解析的硬编码顺序

最关键的是 `getMethodArgumentValues(...)`：

```java
for (int i = 0; i < parameters.length; i++) {
    MethodParameter parameter = parameters[i];
    args[i] = findProvidedArgument(parameter, providedArgs);
    if (args[i] != null) {
        continue;
    }
    if (!this.resolvers.supportsParameter(parameter)) {
        throw new IllegalStateException(...);
    }
    args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory);
}
```

这段已经把顺序写死了：

```text
对每个参数：
1. 先看 providedArgs 里有没有现成值
2. 没有的话，调用 resolvers.supportsParameter(parameter)
3. 如果支持，再调用 resolvers.resolveArgument(...)
4. 所有参数都解析完之后，才会 doInvoke(...)
```

所以对同一个参数来说，顺序一定是：

```text
supportsParameter(...)
  -> resolveArgument(...)
  -> 调用 controller
```

这不是“约定”，而是 Spring MVC 源码里的固定执行流程。

### 3. 为什么会先 supports 再 resolve

因为 Spring 不知道一个参数该交给哪个 resolver。

它必须先问：

```text
“你能不能处理这个参数？”
```

谁答“能”，再让谁去真正解析。

因此：

- `supportsParameter(...)` 是匹配阶段
- `resolveArgument(...)` 是执行阶段

## 三、HandlerMethodArgumentResolverComposite

这个类相当于 resolver 调度器。

### 1. supportsParameter 做了什么

```java
@Override
public boolean supportsParameter(MethodParameter parameter) {
    return getArgumentResolver(parameter) != null;
}
```

看起来简单，但关键在 `getArgumentResolver(parameter)`。

### 2. resolver 是怎么被选中的

```java
public HandlerMethodArgumentResolver getArgumentResolver(MethodParameter parameter) {
    HandlerMethodArgumentResolver result = this.argumentResolverCache.get(parameter);
    if (result == null) {
        for (HandlerMethodArgumentResolver resolver : this.argumentResolvers) {
            if (resolver.supportsParameter(parameter)) {
                result = resolver;
                this.argumentResolverCache.put(parameter, result);
                break;
            }
        }
    }
    return result;
}
```

这里可以提炼出 4 个规则：

1. Spring 会按注册顺序遍历 resolver 列表
2. 第一个 `supportsParameter(parameter)` 返回 `true` 的 resolver 会被选中
3. 一旦选中就 `break`
4. 结果会缓存到 `argumentResolverCache`

这解释了两个常见问题：

### 为什么自定义 resolver 要写得足够精确

因为一旦你的 `supportsParameter(...)` 写得太宽，可能会抢走本来该由别的 resolver 处理的参数。

### 为什么注册顺序有意义

因为是“谁先匹配上谁处理”，而不是所有匹配者一起参与。

### 3. resolveArgument 做了什么

```java
HandlerMethodArgumentResolver resolver = getArgumentResolver(parameter);
if (resolver == null) {
    throw new IllegalArgumentException(...);
}
return resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
```

也就是说：

- `Composite` 自己不负责取值
- 它只负责选 resolver
- 真正取值还是交给被选中的 resolver

## 四、HandlerMethodArgumentResolver 接口语义

这个 SPI 的职责天然就是两阶段：

### supportsParameter(MethodParameter parameter)

回答一个问题：

```text
“这个参数是不是我负责？”
```

### resolveArgument(...)

回答另一个问题：

```text
“既然归我负责，那具体值怎么拿？”
```

你的 `CurrentOperatorArgumentResolver` 就是这个模型：

```java
public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(AuthIdentity.class)
            && CurrentOperator.class.isAssignableFrom(parameter.getParameterType());
}
```

先精准命中：

- 参数上必须有 `@AuthIdentity`
- 参数类型必须是 `CurrentOperator`

然后再在 `resolveArgument(...)` 里从 request attribute 中取值。

## 五、套到你项目里的实际执行顺序

假设 controller 方法是：

```java
public Result<Boolean> logout(@AuthIdentity CurrentOperator currentOperator)
```

那么 Spring MVC 实际会这样跑：

```text
RequestMappingHandlerAdapter.invokeHandlerMethod(...)
  -> 创建 InvocableHandlerMethod
  -> setHandlerMethodArgumentResolvers(this.argumentResolvers)
  -> InvocableHandlerMethod.invokeForRequest(...)
    -> getMethodArgumentValues(...)
      -> Composite.supportsParameter(parameter)
        -> getArgumentResolver(parameter)
          -> 遍历所有 resolver
          -> CurrentOperatorArgumentResolver.supportsParameter(parameter) == true
      -> Composite.resolveArgument(parameter, ...)
        -> getArgumentResolver(parameter)
        -> CurrentOperatorArgumentResolver.resolveArgument(...)
          -> request.getAttribute("currentOperator")
    -> doInvoke(args)
      -> controller.logout(currentOperator)
```

这里最重要的是：

- 先选 resolver
- 再解析参数
- 最后执行 controller

## 六、你应该重点盯住的源码方法

如果你后面准备继续深入，我建议按下面顺序看：

### 第一组：总装配

- `RequestMappingHandlerAdapter.afterPropertiesSet()`
- `RequestMappingHandlerAdapter.getDefaultArgumentResolvers()`
- `RequestMappingHandlerAdapter.invokeHandlerMethod()`

你要回答的问题是：

```text
resolver 链是怎么组起来的？
自定义 resolver 被插到了哪里？
```

### 第二组：参数解析主链

- `InvocableHandlerMethod.invokeForRequest()`
- `InvocableHandlerMethod.getMethodArgumentValues()`

你要回答的问题是：

```text
参数解析和方法调用的先后顺序是什么？
```

### 第三组：resolver 选择

- `HandlerMethodArgumentResolverComposite.supportsParameter()`
- `HandlerMethodArgumentResolverComposite.getArgumentResolver()`
- `HandlerMethodArgumentResolverComposite.resolveArgument()`

你要回答的问题是：

```text
Spring 最终选中了哪个 resolver？
为什么是它？
```

## 七、对当前项目最有价值的结论

### 1. `supportsParameter -> resolveArgument` 是框架规定的

不是经验规则，也不是文档建议，而是 `InvocableHandlerMethod.getMethodArgumentValues(...)` 里明确写死的执行顺序。

### 2. resolver 的匹配是“第一个命中即处理”

所以自定义 resolver：

- 条件要写准
- 不要写成“看到对象类型就全接”

### 3. request attribute + argument resolver 是标准 MVC 适配层做法

你现在这套：

```text
Interceptor 鉴权
  -> request.setAttribute("currentIdentity", ...)
  -> ArgumentResolver 注入 controller 参数
```

完全符合 Spring MVC 的扩展方式，也比应用层直接读 `ThreadLocal` 更干净。

## 八、一句话总结

Spring MVC 对参数注入的源码级规则就是：

```text
RequestMappingHandlerAdapter 负责组装 resolver 链，
InvocableHandlerMethod 负责逐个参数驱动解析，
HandlerMethodArgumentResolverComposite 负责按顺序挑选 resolver，
并严格按照 supportsParameter -> resolveArgument -> controller 调用的顺序执行。
```
