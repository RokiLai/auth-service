package com.example.authservice.identity.usecase.impl;

import com.example.authservice.domain.identity.model.entity.IdentityAccount;
import com.example.authservice.domain.identity.model.entity.IdentitySession;
import com.example.authservice.domain.identity.model.result.AuthenticationResult;
import com.example.authservice.domain.identity.model.result.AuthorizationSnapshot;
import com.example.authservice.domain.identity.repository.IdentitySessionRepository;
import com.example.authservice.domain.identity.service.AuthenticationDomainService;
import com.example.authservice.domain.identity.service.AuthorizationSnapshotProvider;
import com.example.authservice.identity.usecase.LoginUseCase;
import com.example.authservice.identity.usecase.result.LoginResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class LoginUseCaseImpl implements LoginUseCase {

    private final AuthenticationDomainService authenticationDomainService;
    private final IdentitySessionRepository identitySessionRepository;
    private final AuthorizationSnapshotProvider authorizationSnapshotProvider;

    public LoginUseCaseImpl(AuthenticationDomainService authenticationDomainService,
                            IdentitySessionRepository identitySessionRepository,
                            AuthorizationSnapshotProvider authorizationSnapshotProvider) {
        this.authenticationDomainService = authenticationDomainService;
        this.identitySessionRepository = identitySessionRepository;
        this.authorizationSnapshotProvider = authorizationSnapshotProvider;
    }

    @Override
    public LoginResult login(String username, String password) {
        // 认证、替换旧会话、创建新会话这些规则已下沉到领域服务。
        AuthenticationResult authenticationResult = authenticationDomainService.authenticate(username, password);
        IdentityAccount account = authenticationResult.getAccount();
        IdentitySession session = authenticationResult.getSession();

        // 角色和权限在登录时做一次快照，避免每次鉴权都回源查询。
        if (!CollectionUtils.isEmpty(account.getRoleIds())) {
            AuthorizationSnapshot snapshot = authorizationSnapshotProvider.loadByRoleIds(account.getRoleIds());
            session.grantAuthorities(snapshot.getRoles(), snapshot.getPermissions());
        }

        // 应用层负责持久化会话，并把领域结果转换成接口返回对象。
        identitySessionRepository.save(session);
        return new LoginResult(account.getId(), account.getUsername(), account.getEmail(), session.getToken());
    }
}
