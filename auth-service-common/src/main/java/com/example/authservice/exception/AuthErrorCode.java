package com.example.authservice.exception;

import com.roki.exception.code.annotation.ErrorCodeMeta;
import com.roki.exception.code.annotation.ErrorCodeScopeName;
import com.roki.exception.code.definition.DeclarativeErrorCode;

@ErrorCodeScopeName("auth")
public enum AuthErrorCode implements DeclarativeErrorCode {
    @ErrorCodeMeta(detailCode = "001", message = "用户名已存在")
    USERNAME_ALREADY_EXISTS,

    @ErrorCodeMeta(detailCode = "002", message = "用户名不能为空")
    USERNAME_REQUIRED,

    @ErrorCodeMeta(detailCode = "003", message = "密码长度不能少于6位")
    PASSWORD_TOO_SHORT,

    @ErrorCodeMeta(detailCode = "004", message = "邮箱格式不正确")
    EMAIL_INVALID,

    @ErrorCodeMeta(detailCode = "005", message = "旧密码错误")
    OLD_PASSWORD_INCORRECT,

    @ErrorCodeMeta(detailCode = "006", message = "角色授权参数不能为空")
    ROLE_AUTHORIZE_PARAM_INVALID,

    @ErrorCodeMeta(detailCode = "101", message = "用户名或密码错误")
    LOGIN_FAILED,

    @ErrorCodeMeta(detailCode = "102", message = "缺少Token，请先登录")
    TOKEN_MISSING,

    @ErrorCodeMeta(detailCode = "103", message = "Token已过期，请重新登录")
    TOKEN_EXPIRED,

    @ErrorCodeMeta(detailCode = "104", message = "Token无效")
    TOKEN_INVALID,

    @ErrorCodeMeta(detailCode = "901", message = "JSON处理失败")
    JSON_PROCESS_ERROR
}
