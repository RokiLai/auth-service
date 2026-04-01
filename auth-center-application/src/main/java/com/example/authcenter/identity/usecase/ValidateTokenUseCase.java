package com.example.authcenter.identity.usecase;

import com.example.authcenter.identity.usecase.result.ValidatedUserResult;

public interface ValidateTokenUseCase {

    ValidatedUserResult validate(String rawToken);
}
