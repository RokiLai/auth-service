package com.example.authservice.domain.identity.model.entity;

import org.springframework.stereotype.Component;

@Component
public class IdentitySessionFactory {

    /**
     * 基于已认证账号创建新的登录会话，确保会话创建入口只保留在领域工厂里。
     * Creates a new login session for an authenticated account so session creation stays behind a dedicated domain factory.
     */
    public IdentitySession createFor(IdentityAccount account, String sessionId, String token) {
        return new IdentitySession(sessionId, account.getId(), account.getUsername(), token);
    }

    /**
     * 从持久化层恢复会话实体时统一走该工厂，避免实体暴露静态恢复方法。
     * Reconstitutes the session from persistence through a factory instead of static restore methods on the entity.
     */
    public IdentitySession restore(String sessionId,
                                   Long accountId,
                                   String username,
                                   String token) {
        return new IdentitySession(sessionId, accountId, username, token);
    }
}
