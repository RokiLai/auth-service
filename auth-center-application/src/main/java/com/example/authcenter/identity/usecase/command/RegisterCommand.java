package com.example.authcenter.identity.usecase.command;

/**
 * 注册用例的输入命令。
 * Command object for the register use case.
 */
public record RegisterCommand(
        String username,
        String password,
        String email
) {
}
