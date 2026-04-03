package com.example.authcenter.identity.usecase.impl;

import com.example.authcenter.application.context.CurrentOperator;
import com.example.authcenter.domain.identity.model.entity.IdentitySession;
import com.example.authcenter.domain.identity.model.valueobject.TokenClaims;
import com.example.authcenter.domain.identity.repository.IdentitySessionRepository;
import com.example.authcenter.domain.identity.service.IdentityTokenProvider;
import com.example.authcenter.exception.auth.TokenExpiredException;
import com.example.authcenter.exception.auth.TokenInvalidException;
import com.example.authcenter.identity.usecase.AuthenticateUseCase;
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
    public CurrentOperator authenticate(String rawToken) {
        try {
            TokenClaims claims = identityTokenProvider.parse(rawToken);
            String sessionId = claims.sessionId();
            if (sessionId == null || sessionId.isBlank()) {
                throw new TokenInvalidException();
            }

            IdentitySession session = identitySessionRepository.findBySessionId(sessionId);
            // JWT 合法还不够，必须同时命中当前有效会话，才能认为登录态有效。
            if (session == null) {
                throw new TokenExpiredException();
            }

            return new CurrentOperator(
                    session.getAccountId(),
                    session.getUsername(),
                    session.getSessionId()
            );
        } catch (JwtException e) {
            throw new TokenInvalidException();
        }
    }
}
