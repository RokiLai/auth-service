package com.example.authcenter.config;

import com.example.authcenter.application.context.CurrentOperator;
import com.example.authcenter.annotation.PassToken;
import com.example.authcenter.exception.auth.TokenInvalidException;
import com.example.authcenter.exception.auth.TokenMissingException;
import com.example.authcenter.identity.usecase.AuthenticateUseCase;
import com.roki.exception.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    // 认证通过后把当前操作者放进 request，供参数解析器和控制器读取。
    // Stores the current operator on the request for argument resolution and controller access.
    public static final String CURRENT_OPERATOR_ATTR = "currentOperator";
    private static final Logger logger = LoggerFactory.getLogger(JwtInterceptor.class); // 添加日志记录器

    private final AuthenticateUseCase authenticateUseCase;

    public JwtInterceptor(AuthenticateUseCase authenticateUseCase) {
        this.authenticateUseCase = authenticateUseCase;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            logger.info("非控制器请求，直接放行");
            return true; // 放行非控制器请求
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        // 跳过 @PassToken 标记的接口
        if (method.isAnnotationPresent(PassToken.class)) {
            PassToken passToken = method.getAnnotation(PassToken.class);
            if (passToken.required()) {
                logger.info("接口 {} 被 @PassToken 标记，跳过验证", method.getName());
                return true;
            }
        }

        String authorizationHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorizationHeader)) {
            logger.warn("请求缺少 Token，拒绝访问");
            throw new TokenMissingException();
        }

        try {
            String token = resolveToken(authorizationHeader);
            CurrentOperator currentOperator = authenticateUseCase.authenticate(token);
            logger.info("Token 验证通过，用户: {}", currentOperator.username());
            // 接口层通过 request attribute 传递当前操作者，避免应用层依赖 ThreadLocal。
            // Passes the current operator through request attributes so upper layers no longer depend on ThreadLocal.
            request.setAttribute("username", currentOperator.username());
            request.setAttribute(CURRENT_OPERATOR_ATTR, currentOperator);
        } catch (BusinessException e) {
            logger.warn("Token 校验未通过: {}", e.getMessage());
            throw e;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
    }

    private String resolveToken(String authorizationHeader) {
        // 这里只负责解析 Bearer token 载荷，不承担业务鉴权职责。
        // This only parses the Bearer token value and does not perform business authentication.
        String candidate = authorizationHeader.trim();
        if (!StringUtils.hasText(candidate)) {
            throw new TokenMissingException();
        }
        if (!candidate.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new TokenInvalidException();
        }

        String bearerToken = candidate.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(bearerToken)) {
            throw new TokenInvalidException();
        }
        return bearerToken;
    }
}
