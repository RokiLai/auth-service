package com.example.authcenter.exception.auth;

import com.example.authcenter.exception.AuthErrorCode;

/**
 * token 格式错误、Bearer 协议错误或 JWT 解析失败时抛出。
 */
public class TokenInvalidException extends AuthBusinessException {

    public TokenInvalidException() {
        super(AuthErrorCode.TOKEN_INVALID);
    }
}
