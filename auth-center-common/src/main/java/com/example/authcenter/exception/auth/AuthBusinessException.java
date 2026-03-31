package com.example.authcenter.exception.auth;

import com.example.authcenter.exception.AuthErrorCode;
import com.roki.exception.exception.BusinessException;

/**
 * 认证域异常基类：保留统一错误码响应，同时让代码层面具备业务语义。
 */
public abstract class AuthBusinessException extends BusinessException {

    protected AuthBusinessException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
