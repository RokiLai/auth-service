package com.example.authservice.config;

import com.example.authservice.application.context.CurrentOperator;
import com.example.authservice.annotation.AuthIdentity;
import com.example.authservice.exception.auth.TokenInvalidException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentOperatorArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 只解析显式声明了 @AuthIdentity 的 CurrentOperator 参数，避免误注入。
        // Resolves only explicitly annotated CurrentOperator parameters to avoid accidental injection.
        return parameter.hasParameterAnnotation(AuthIdentity.class)
                && CurrentOperator.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        // 当前操作者由拦截器提前放入 request；这里负责做一次安全提取和类型校验。
        // The interceptor stores the current operator on the request; this resolver safely extracts and validates it.
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new TokenInvalidException();
        }
        Object currentOperator = request.getAttribute(JwtInterceptor.CURRENT_OPERATOR_ATTR);
        if (!(currentOperator instanceof CurrentOperator)) {
            throw new TokenInvalidException();
        }
        return currentOperator;
    }
}
