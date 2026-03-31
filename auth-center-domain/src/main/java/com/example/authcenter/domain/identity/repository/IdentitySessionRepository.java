package com.example.authcenter.domain.identity.repository;

import com.example.authcenter.domain.identity.model.entity.IdentitySession;

public interface IdentitySessionRepository {

    void save(IdentitySession session);

    IdentitySession findBySessionId(String sessionId);

    String findSessionIdByAccountId(Long accountId);

    IdentitySession findByAccountId(Long accountId);

    void deleteBySessionId(String sessionId);

    void deleteByAccountId(Long accountId);
}
