package com.example.authservice.config;

import com.example.authservice.annotation.AuthIdentity;
import com.example.authservice.exception.auth.TokenInvalidException;
import com.example.authservice.domain.identity.model.result.CurrentIdentity;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class CurrentIdentityArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 只解析显式声明了 @AuthIdentity 的 CurrentIdentity 参数，避免误注入。
        // Resolves only explicitly annotated CurrentIdentity parameters to avoid accidental injection.
        return parameter.hasParameterAnnotation(AuthIdentity.class)
                && CurrentIdentity.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        // 当前身份由拦截器提前放入 request；这里负责做一次安全提取和类型校验。
        // The interceptor stores identity on the request; this resolver safely extracts and validates it.
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new TokenInvalidException();
        }
        Object currentIdentity = request.getAttribute(JwtInterceptor.CURRENT_IDENTITY_ATTR);
        if (!(currentIdentity instanceof CurrentIdentity)) {
            throw new TokenInvalidException();
        }
        return currentIdentity;
    }
}
