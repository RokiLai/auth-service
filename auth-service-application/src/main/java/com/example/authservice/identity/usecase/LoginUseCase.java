package com.example.authservice.identity.usecase;

import com.example.authservice.identity.usecase.result.LoginResult;

public interface LoginUseCase {

    LoginResult login(String username, String password);
}
