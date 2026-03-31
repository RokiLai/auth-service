package com.example.authcenter.exception.auth;

import com.example.authcenter.exception.AuthErrorCode;

/**
 * 注册时用户名已存在。
 */
public class UsernameAlreadyExistsException extends AuthBusinessException {

    public UsernameAlreadyExistsException() {
        super(AuthErrorCode.USERNAME_ALREADY_EXISTS);
    }
}
