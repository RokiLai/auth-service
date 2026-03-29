package com.example.authservice.identity.usecase.command;

import com.example.authservice.application.context.CurrentOperator;

/**
 * 登出用例的输入命令。
 * Command object for the logout use case.
 */
public record LogoutCommand(CurrentOperator operator) {
}
