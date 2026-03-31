package com.example.authcenter.identity.usecase;

import com.example.authcenter.identity.usecase.result.LoginResult;

public interface LoginUseCase {

    LoginResult login(String username, String password);
}
