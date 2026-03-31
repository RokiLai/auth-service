package com.example.authcenter.identity.usecase;

import com.example.authcenter.identity.usecase.command.LogoutCommand;

public interface LogoutUseCase {

    boolean logout(LogoutCommand command);
}
