package com.example.authcenter.exception.auth;

import com.example.authcenter.exception.AuthErrorCode;

/**
 * 修改密码时旧密码校验失败。
 */
public class OldPasswordIncorrectException extends AuthBusinessException {

    public OldPasswordIncorrectException() {
        super(AuthErrorCode.OLD_PASSWORD_INCORRECT);
    }
}
