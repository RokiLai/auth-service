package com.example.authcenter.exception.auth;

import com.example.authcenter.exception.AuthErrorCode;

/**
 * token 对应的登录会话已失效或已被替换时抛出。
 */
public class TokenExpiredException extends AuthBusinessException {

    public TokenExpiredException() {
        super(AuthErrorCode.TOKEN_EXPIRED);
    }
}
