package com.example.authcenter.identity.usecase;

import com.example.authcenter.application.context.CurrentOperator;

public interface AuthenticateUseCase {

    CurrentOperator authenticate(String rawToken);
}
