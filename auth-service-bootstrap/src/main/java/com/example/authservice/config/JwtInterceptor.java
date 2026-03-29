package com.example.authservice.config;

import com.example.authservice.annotation.PassToken;
import com.example.authservice.auth.IdentityContext;
import com.example.authservice.auth.IdentityContextHolder;
import com.example.authservice.domain.identity.model.CurrentIdentity;
import com.example.authservice.exception.AuthErrorCode;
import com.example.authservice.identity.usecase.AuthenticateUseCase;
import com.roki.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Logger logger = LoggerFactory.getLogger(JwtInterceptor.class); // 添加日志记录器

    @Autowired
    private AuthenticateUseCase authenticateUseCase;

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
            throw new BusinessException(AuthErrorCode.TOKEN_MISSING);
        }

        try {
            String token = resolveToken(authorizationHeader);
            CurrentIdentity currentIdentity = authenticateUseCase.authenticate(token);
            IdentityContext identityContext = new IdentityContext();
            identityContext.setId(currentIdentity.getId());
            identityContext.setUsername(currentIdentity.getUsername());
            identityContext.setSessionId(currentIdentity.getSessionId());
            identityContext.setToken(currentIdentity.getToken());
            identityContext.setRoles(currentIdentity.getRoles());
            identityContext.setPermissions(currentIdentity.getPermissions());

            logger.info("Token 验证通过，用户: {}", currentIdentity.getUsername());
            request.setAttribute("username", currentIdentity.getUsername());
            IdentityContextHolder.set(identityContext);
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
        IdentityContextHolder.clear();
    }

    private String resolveToken(String authorizationHeader) {
        String candidate = authorizationHeader.trim();
        if (!StringUtils.hasText(candidate)) {
            throw new BusinessException(AuthErrorCode.TOKEN_MISSING);
        }
        if (!candidate.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
        }

        String bearerToken = candidate.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(bearerToken)) {
            throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
        }
        return bearerToken;
    }
}
