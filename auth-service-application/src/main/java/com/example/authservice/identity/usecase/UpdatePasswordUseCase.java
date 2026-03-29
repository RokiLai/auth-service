package com.example.authservice.identity.usecase;

import com.example.authservice.identity.usecase.command.UpdatePasswordCommand;

public interface UpdatePasswordUseCase {

    boolean updatePassword(UpdatePasswordCommand command);
}
