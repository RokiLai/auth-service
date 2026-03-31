package com.example.authcenter.exception.auth;

import com.example.authcenter.exception.AuthErrorCode;

/**
 * JSON 序列化或反序列化失败时抛出。
 */
public class JsonProcessException extends AuthBusinessException {

    public JsonProcessException() {
        super(AuthErrorCode.JSON_PROCESS_ERROR);
    }
}
