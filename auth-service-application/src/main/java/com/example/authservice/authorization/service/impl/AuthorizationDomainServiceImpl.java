package com.example.authservice.authorization.service.impl;

import com.example.authservice.domain.authorization.model.Permission;
import com.example.authservice.domain.authorization.model.Role;
import com.example.authservice.domain.authorization.repository.PermissionRepository;
import com.example.authservice.domain.authorization.repository.RolePermissionRepository;
import com.example.authservice.domain.authorization.repository.RoleRepository;
import com.example.authservice.domain.authorization.service.AuthorizationDomainService;
import com.example.authservice.domain.identity.model.valueobject.AuthorizationSnapshot;
import com.example.authservice.exception.auth.RoleAuthorizeParamInvalidException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthorizationDomainServiceImpl implements AuthorizationDomainService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public AuthorizationDomainServiceImpl(RoleRepository roleRepository,
                                          PermissionRepository permissionRepository,
                                          RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    public void authorizeRole(Long roleId, Set<Long> permissionIds) {
        // 写侧先做参数防御，避免无意义地进入聚合与仓储层。
        // Guard invalid write commands early before touching aggregates or repositories.
        if (roleId == null || permissionIds == null) {
            throw new RoleAuthorizeParamInvalidException();
        }

        Role role = roleRepository.findById(roleId);
        if (role == null) {
            return;
        }

        // 只允许持久化层真实存在的权限进入角色，避免脏授权数据落库。
        // Only permissions that actually exist may be attached to the role, preventing dirty authorization state.
        Set<Long> distinctPermissionIds = new LinkedHashSet<>(permissionIds);
        List<Long> existingPermissionIds = permissionRepository.findByIds(List.copyOf(distinctPermissionIds)).stream()
                .map(Permission::id)
                .toList();
        if (existingPermissionIds.size() != distinctPermissionIds.size()) {
            throw new RoleAuthorizeParamInvalidException();
        }

        role.authorize(new LinkedHashSet<>(existingPermissionIds));
        roleRepository.save(role);
    }

    @Override
    public AuthorizationSnapshot buildSnapshot(List<Long> roleIds) {
        // 读侧统一在授权域完成聚合，identity 只消费结果快照。
        // The read-side aggregation stays inside the authorization domain; identity only consumes the snapshot.
        if (CollectionUtils.isEmpty(roleIds)) {
            return new AuthorizationSnapshot(List.of(), List.of());
        }

        List<String> roles = roleRepository.findCodesByIds(roleIds);
        List<Long> permissionIds = rolePermissionRepository.findPermissionIdsByRoleIds(roleIds);
        List<String> permissions = CollectionUtils.isEmpty(permissionIds)
                ? List.of()
                : permissionRepository.findCodesByIds(permissionIds);
        return new AuthorizationSnapshot(roles, permissions);
    }
}
