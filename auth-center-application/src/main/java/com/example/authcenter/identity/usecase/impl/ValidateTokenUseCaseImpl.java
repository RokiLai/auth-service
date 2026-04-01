package com.example.authcenter.identity.usecase.impl;

import com.example.authcenter.application.context.CurrentOperator;
import com.example.authcenter.domain.identity.model.entity.IdentityAccount;
import com.example.authcenter.domain.identity.repository.IdentityAccountRepository;
import com.example.authcenter.exception.auth.TokenInvalidException;
import com.example.authcenter.identity.usecase.AuthenticateUseCase;
import com.example.authcenter.identity.usecase.ValidateTokenUseCase;
import com.example.authcenter.identity.usecase.result.ValidatedUserResult;
import org.springframework.stereotype.Service;

@Service
public class ValidateTokenUseCaseImpl implements ValidateTokenUseCase {

    private final AuthenticateUseCase authenticateUseCase;
    private final IdentityAccountRepository identityAccountRepository;

    public ValidateTokenUseCaseImpl(AuthenticateUseCase authenticateUseCase,
                                    IdentityAccountRepository identityAccountRepository) {
        this.authenticateUseCase = authenticateUseCase;
        this.identityAccountRepository = identityAccountRepository;
    }

    @Override
    public ValidatedUserResult validate(String rawToken) {
        CurrentOperator currentOperator = authenticateUseCase.authenticate(rawToken);
        IdentityAccount account = identityAccountRepository.findById(currentOperator.id());
        if (account == null) {
            throw new TokenInvalidException();
        }
        return new ValidatedUserResult(
                account.getId(),
                account.getUsername(),
                account.getEmail()
        );
    }
}
