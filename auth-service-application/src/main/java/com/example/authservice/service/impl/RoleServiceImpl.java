package com.example.authservice.service.impl;

import com.example.authservice.domain.authorization.model.Role;
import com.example.authservice.domain.authorization.repository.RoleRepository;
import com.example.authservice.exception.auth.RoleAuthorizeParamInvalidException;

import java.util.ArrayList;
import java.util.Set;

public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
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
