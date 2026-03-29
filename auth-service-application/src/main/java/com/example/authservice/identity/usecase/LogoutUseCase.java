package com.example.authservice.identity.usecase;

import com.example.authservice.identity.usecase.command.LogoutCommand;

public interface LogoutUseCase {

    boolean logout(LogoutCommand command);
}
