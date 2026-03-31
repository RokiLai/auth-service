package com.example.authcenter.identity.usecase.impl;

import com.example.authcenter.domain.identity.model.entity.IdentityAccount;
import com.example.authcenter.domain.identity.model.entity.IdentitySession;
import com.example.authcenter.domain.identity.model.context.AuthenticatedIdentity;
import com.example.authcenter.domain.identity.repository.IdentitySessionRepository;
import com.example.authcenter.domain.identity.service.AuthenticationDomainService;
import com.example.authcenter.identity.usecase.LoginUseCase;
import com.example.authcenter.identity.usecase.result.LoginResult;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCaseImpl implements LoginUseCase {

    private final AuthenticationDomainService authenticationDomainService;
    private final IdentitySessionRepository identitySessionRepository;

    public LoginUseCaseImpl(AuthenticationDomainService authenticationDomainService,
                            IdentitySessionRepository identitySessionRepository) {
        this.authenticationDomainService = authenticationDomainService;
        this.identitySessionRepository = identitySessionRepository;
    }

    @Override
    public LoginResult login(String username, String password) {
        // 认证、替换旧会话、创建新会话这些规则已下沉到领域服务。
        AuthenticatedIdentity authenticatedIdentity = authenticationDomainService.authenticate(username, password);
        IdentityAccount account = authenticatedIdentity.account();
        IdentitySession session = authenticatedIdentity.session();

        // 应用层负责持久化会话，并把领域结果转换成接口返回对象。
        identitySessionRepository.save(session);
        return new LoginResult(account.getId(), account.getUsername(), account.getEmail(), session.getToken());
    }
}
