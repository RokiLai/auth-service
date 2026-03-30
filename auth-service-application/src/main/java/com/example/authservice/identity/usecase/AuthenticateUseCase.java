package com.example.authservice.identity.usecase;

import com.example.authservice.application.context.CurrentOperator;

public interface AuthenticateUseCase {

    CurrentOperator authenticate(String rawToken);
}
