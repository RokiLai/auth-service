package com.example.authcenter.exception.auth;

import com.example.authcenter.exception.AuthErrorCode;

/**
 * 邮箱为空或格式不合法时抛出。
 */
public class EmailInvalidException extends AuthBusinessException {

    public EmailInvalidException() {
        super(AuthErrorCode.EMAIL_INVALID);
    }
}
