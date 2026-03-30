package com.example.authservice.authorization.usecase.impl;

import com.example.authservice.authorization.usecase.AuthorizeRoleUseCase;
import com.example.authservice.domain.authorization.model.Role;
import com.example.authservice.domain.authorization.repository.RoleRepository;
import com.example.authservice.exception.auth.RoleAuthorizeParamInvalidException;

import java.util.ArrayList;
import java.util.Set;

public class AuthorizeRoleUseCaseImpl implements AuthorizeRoleUseCase {

    private final RoleRepository roleRepository;

    public AuthorizeRoleUseCaseImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void batchAuthorize(Long roleId, Set<Long> permissionIds) {
        if (roleId == null || permissionIds == null) {
            throw new RoleAuthorizeParamInvalidException();
        }
        Role role = roleRepository.selectById(roleId);
        if (role == null) {
            return;
        }
        role.addPermissions(new ArrayList<>(permissionIds));
        roleRepository.updateRolePermission(role);
    }
}
