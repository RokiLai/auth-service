package com.example.authcenter.identity.usecase.command;

import com.example.authcenter.application.context.CurrentOperator;

/**
 * 登出用例的输入命令。
 * Command object for the logout use case.
 */
public record LogoutCommand(CurrentOperator operator) {
}
