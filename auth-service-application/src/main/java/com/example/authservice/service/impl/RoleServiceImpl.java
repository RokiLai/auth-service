package com.example.authservice.service.impl;

import com.example.authservice.domain.model.Role;
import com.example.authservice.domain.repo.RoleRepo;
import com.example.authservice.exception.auth.RoleAuthorizeParamInvalidException;

import java.util.ArrayList;
import java.util.Set;

public class RoleServiceImpl implements RoleService {

    private final RoleRepo roleRepo;

    public RoleServiceImpl(RoleRepo roleRepo) {
        this.roleRepo = roleRepo;
    }

    @Override
    public void batchAuthorize(Long roleId, Set<Long> permissionIds) {
        if (roleId == null || permissionIds == null) {
            throw new RoleAuthorizeParamInvalidException();
        }
        Role role = roleRepo.selectById(roleId);
        if (role == null) {
            return;
        }
        role.addPermissions(new ArrayList<>(permissionIds));
        roleRepo.updateRolePermission(role);
    }
}
