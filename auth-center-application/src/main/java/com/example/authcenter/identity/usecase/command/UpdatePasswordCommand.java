package com.example.authcenter.identity.usecase.command;

import com.example.authcenter.application.context.CurrentOperator;

/**
 * 修改密码用例的输入命令。
 * Command object for the update-password use case.
 */
public record UpdatePasswordCommand(
        CurrentOperator operator,
        String oldPassword,
        String newPassword
) {
}
