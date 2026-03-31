package com.example.authcenter.identity.usecase;

import com.example.authcenter.identity.usecase.command.RegisterCommand;

public interface RegisterUseCase {

    boolean register(RegisterCommand command);
}
