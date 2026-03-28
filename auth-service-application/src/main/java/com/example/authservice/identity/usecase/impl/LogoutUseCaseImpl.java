package com.example.authservice.identity.usecase.impl;

import com.example.authservice.auth.IdentityContext;
import com.example.authservice.auth.IdentityContextHolder;
import com.example.authservice.domain.identity.repository.IdentitySessionRepository;
import com.example.authservice.exception.AuthErrorCode;
import com.example.authservice.identity.usecase.LogoutUseCase;
import com.roki.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class LogoutUseCaseImpl implements LogoutUseCase {

    private final IdentitySessionRepository identitySessionRepository;

    public LogoutUseCaseImpl(IdentitySessionRepository identitySessionRepository) {
        this.identitySessionRepository = identitySessionRepository;
    }

    @Override
    public boolean logout() {
        IdentityContext currentAccount = IdentityContextHolder.get();
        if (currentAccount == null || currentAccount.getId() == null || currentAccount.getSessionId() == null || currentAccount.getSessionId().isBlank()) {
            throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
        }

        identitySessionRepository.deleteBySessionId(currentAccount.getSessionId());

        String boundSessionId = identitySessionRepository.findSessionIdByAccountId(currentAccount.getId());
        if (Objects.equals(boundSessionId, currentAccount.getSessionId())) {
            identitySessionRepository.deleteByAccountId(currentAccount.getId());
        }
        return true;
    }
}
