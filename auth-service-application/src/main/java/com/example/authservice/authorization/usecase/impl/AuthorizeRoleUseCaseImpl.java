package com.example.authservice.authorization.usecase.impl;

import com.example.authservice.authorization.usecase.AuthorizeRoleUseCase;
import com.example.authservice.authorization.usecase.command.AuthorizeRoleCommand;
import com.example.authservice.domain.authorization.service.AuthorizationDomainService;
import org.springframework.stereotype.Service;

@Service
public class AuthorizeRoleUseCaseImpl implements AuthorizeRoleUseCase {

    private final AuthorizationDomainService authorizationDomainService;

    public AuthorizeRoleUseCaseImpl(AuthorizationDomainService authorizationDomainService) {
        this.authorizationDomainService = authorizationDomainService;
    }

    @Override
    public void batchAuthorize(AuthorizeRoleCommand command) {
        authorizationDomainService.authorizeRole(
                command == null ? null : command.roleId(),
                command == null ? null : command.permissionIds()
        );
    }
}
