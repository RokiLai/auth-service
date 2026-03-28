package com.example.authservice.identity.usecase.impl;

import com.example.authservice.domain.identity.model.CurrentIdentity;
import com.example.authservice.domain.identity.model.IdentitySession;
import com.example.authservice.domain.identity.model.TokenClaims;
import com.example.authservice.domain.identity.repository.IdentitySessionRepository;
import com.example.authservice.domain.identity.service.IdentityTokenProvider;
import com.example.authservice.exception.AuthErrorCode;
import com.example.authservice.identity.usecase.AuthenticateUseCase;
import com.roki.exception.BusinessException;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AuthenticateUseCaseImpl implements AuthenticateUseCase {

    private final IdentityTokenProvider identityTokenProvider;
    private final IdentitySessionRepository identitySessionRepository;

    public AuthenticateUseCaseImpl(IdentityTokenProvider identityTokenProvider,
                                   IdentitySessionRepository identitySessionRepository) {
        this.identityTokenProvider = identityTokenProvider;
        this.identitySessionRepository = identitySessionRepository;
    }

    @Override
    public CurrentIdentity authenticate(String rawToken) {
        try {
            TokenClaims claims = identityTokenProvider.parse(rawToken);
            String sessionId = claims.getSessionId();
            if (sessionId == null || sessionId.isBlank()) {
                throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
            }

            IdentitySession session = identitySessionRepository.findBySessionId(sessionId);
            if (session == null || !Objects.equals(session.getToken(), rawToken)) {
                throw new BusinessException(AuthErrorCode.TOKEN_EXPIRED);
            }

            CurrentIdentity identity = new CurrentIdentity();
            identity.setId(session.getAccountId());
            identity.setUsername(session.getUsername());
            identity.setSessionId(session.getSessionId());
            identity.setToken(session.getToken());
            identity.setRoles(session.getRoles());
            identity.setPermissions(session.getPermissions());
            return identity;
        } catch (JwtException e) {
            throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
        }
    }
}
