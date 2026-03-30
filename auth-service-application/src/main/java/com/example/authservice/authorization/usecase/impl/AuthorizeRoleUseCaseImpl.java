package com.example.authservice.authorization.usecase.impl;

import com.example.authservice.authorization.usecase.AuthorizeRoleUseCase;
import com.example.authservice.domain.authorization.service.AuthorizationDomainService;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthorizeRoleUseCaseImpl implements AuthorizeRoleUseCase {

    private final AuthorizationDomainService authorizationDomainService;

    public AuthorizeRoleUseCaseImpl(AuthorizationDomainService authorizationDomainService) {
        this.authorizationDomainService = authorizationDomainService;
    }

    @Override
    public void batchAuthorize(Long roleId, Set<Long> permissionIds) {
        authorizationDomainService.authorizeRole(roleId, permissionIds);
    }
}
