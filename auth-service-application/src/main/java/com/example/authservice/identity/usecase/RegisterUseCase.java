package com.example.authservice.identity.usecase;

import com.example.authservice.identity.usecase.command.RegisterCommand;

public interface RegisterUseCase {

    boolean register(RegisterCommand command);
}
