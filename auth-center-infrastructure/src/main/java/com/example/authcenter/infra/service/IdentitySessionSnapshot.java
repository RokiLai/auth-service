package com.example.authcenter.infra.service;

import com.example.authcenter.domain.identity.model.entity.IdentitySession;

public record IdentitySessionSnapshot(
        String sessionId,
        Long accountId,
        String username
) {

    public static IdentitySessionSnapshot from(IdentitySession session) {
        return new IdentitySessionSnapshot(
                session.getSessionId(),
                session.getAccountId(),
                session.getUsername()
        );
    }
}
