package com.example.authservice.identity.usecase.impl;

import com.example.authservice.domain.identity.model.entity.IdentitySession;
import com.example.authservice.domain.identity.model.valueobject.TokenClaims;
import com.example.authservice.domain.identity.repository.IdentitySessionRepository;
import com.example.authservice.domain.identity.service.IdentityTokenProvider;
import com.example.authservice.exception.auth.TokenExpiredException;
import com.example.authservice.exception.auth.TokenInvalidException;
import com.example.authservice.identity.query.CurrentIdentity;
import com.example.authservice.identity.usecase.AuthenticateUseCase;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

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
            String sessionId = claims.sessionId();
            if (sessionId == null || sessionId.isBlank()) {
                throw new TokenInvalidException();
            }

            IdentitySession session = identitySessionRepository.findBySessionId(sessionId);
            // JWT 合法还不够，必须同时命中当前有效会话，才能认为登录态有效。
            if (session == null || !session.matchesToken(rawToken)) {
                throw new TokenExpiredException();
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
            throw new TokenInvalidException();
        }
    }
}
