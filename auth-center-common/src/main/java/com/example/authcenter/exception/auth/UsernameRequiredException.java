package com.example.authcenter.exception.auth;

import com.example.authcenter.exception.AuthErrorCode;

/**
 * 创建账号时用户名为空。
 */
public class UsernameRequiredException extends AuthBusinessException {

    public UsernameRequiredException() {
        super(AuthErrorCode.USERNAME_REQUIRED);
    }
}
