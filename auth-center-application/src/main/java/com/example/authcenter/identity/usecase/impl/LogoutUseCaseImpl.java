package com.example.authcenter.identity.usecase.impl;

import com.example.authcenter.application.context.CurrentOperator;
import com.example.authcenter.domain.identity.repository.IdentitySessionRepository;
import com.example.authcenter.exception.auth.TokenInvalidException;
import com.example.authcenter.identity.usecase.LogoutUseCase;
import com.example.authcenter.identity.usecase.command.LogoutCommand;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
public class LogoutUseCaseImpl implements LogoutUseCase {

    private final IdentitySessionRepository identitySessionRepository;

    public LogoutUseCaseImpl(IdentitySessionRepository identitySessionRepository) {
        this.identitySessionRepository = identitySessionRepository;
    }

    @Override
    public boolean logout(LogoutCommand command) {
        CurrentOperator operator = command == null ? null : command.operator();
        if (operator == null || operator.id() == null || !StringUtils.hasText(operator.sessionId())) {
            throw new TokenInvalidException();
        }

        identitySessionRepository.deleteBySessionId(operator.sessionId());

        String boundSessionId = identitySessionRepository.findSessionIdByAccountId(operator.id());
        if (Objects.equals(boundSessionId, operator.sessionId())) {
            identitySessionRepository.deleteByAccountId(operator.id());
        }
        return true;
    }
}
