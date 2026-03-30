package com.example.authservice.domain.identity.service.impl;

import com.example.authservice.domain.authorization.service.AuthorizationDomainService;
import com.example.authservice.domain.identity.model.entity.IdentityAccount;
import com.example.authservice.domain.identity.model.entity.IdentitySessionFactory;
import com.example.authservice.domain.identity.model.entity.IdentitySession;
import com.example.authservice.domain.identity.model.context.AuthenticatedIdentity;
import com.example.authservice.domain.identity.model.valueobject.AuthorizationSnapshot;
import com.example.authservice.domain.identity.model.valueobject.RawPassword;
import com.example.authservice.domain.identity.repository.IdentityAccountRepository;
import com.example.authservice.domain.identity.repository.IdentitySessionRepository;
import com.example.authservice.domain.identity.service.AuthenticationDomainService;
import com.example.authservice.domain.identity.service.IdentityTokenProvider;
import com.example.authservice.domain.identity.service.PasswordHasher;
import com.example.authservice.exception.auth.AuthenticationFailedException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.UUID;

@Service
public class AuthenticationDomainServiceImpl implements AuthenticationDomainService {

    private final IdentityAccountRepository identityAccountRepository;
    private final IdentitySessionRepository identitySessionRepository;
    private final IdentityTokenProvider identityTokenProvider;
    private final PasswordHasher passwordHasher;
    private final IdentitySessionFactory identitySessionFactory;
    private final AuthorizationDomainService authorizationDomainService;

    public AuthenticationDomainServiceImpl(IdentityAccountRepository identityAccountRepository,
                                           IdentitySessionRepository identitySessionRepository,
                                           IdentityTokenProvider identityTokenProvider,
                                           PasswordHasher passwordHasher,
                                           IdentitySessionFactory identitySessionFactory,
                                           AuthorizationDomainService authorizationDomainService) {
        this.identityAccountRepository = identityAccountRepository;
        this.identitySessionRepository = identitySessionRepository;
        this.identityTokenProvider = identityTokenProvider;
        this.passwordHasher = passwordHasher;
        this.identitySessionFactory = identitySessionFactory;
        this.authorizationDomainService = authorizationDomainService;
    }

    @Override
    public AuthenticatedIdentity authenticate(String username, String password) {
        // 先确认登录主体存在且密码匹配。
        IdentityAccount account = identityAccountRepository.findByUsername(username);
        if (account == null || !account.matchPassword(new RawPassword(password), passwordHasher)) {
            throw new AuthenticationFailedException();
        }

        // 当前实现采用单点登录策略，新登录会替换掉旧会话。
        IdentitySession oldSession = identitySessionRepository.findByAccountId(account.getId());
        if (oldSession != null && oldSession.getSessionId() != null && !oldSession.getSessionId().isBlank()) {
            identitySessionRepository.deleteBySessionId(oldSession.getSessionId());
            identitySessionRepository.deleteByAccountId(account.getId());
        }

        // 认证成功后生成新的会话标识和 token，并组装会话实体。
        String sessionId = UUID.randomUUID().toString();
        String token = identityTokenProvider.issue(account.getId(), username, sessionId);
        IdentitySession session = identitySessionFactory.createFor(account, sessionId, token);

        // 授权快照属于登录成功后附着到会话上的领域规则，不应由应用层回填。
        // Attaching the authorization snapshot to the session is part of the login domain rule, not application-layer orchestration.
        if (!CollectionUtils.isEmpty(account.getRoleIds())) {
            AuthorizationSnapshot snapshot = authorizationDomainService.buildSnapshot(account.getRoleIds());
            session.grantAuthorities(snapshot.roles(), snapshot.permissions());
        }

        return new AuthenticatedIdentity(account, session);
    }
}
