package com.example.authservice.infra.identity.repository;

import com.example.authservice.domain.identity.model.valueobject.AuthorizationSnapshot;
import com.example.authservice.domain.identity.repository.IdentityAuthorizationRepository;
import com.example.authservice.infra.authorization.mapper.PermissionMapper;
import com.example.authservice.infra.authorization.mapper.RoleMapper;
import com.example.authservice.infra.authorization.mapper.RolePermissionMapper;
import com.example.authservice.infra.authorization.po.PermissionPO;
import com.example.authservice.infra.authorization.po.RolePO;
import com.example.authservice.infra.authorization.po.RolePermissionPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Repository
public class IdentityAuthorizationRepositoryImpl implements IdentityAuthorizationRepository {

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    public IdentityAuthorizationRepositoryImpl(RoleMapper roleMapper,
                                               RolePermissionMapper rolePermissionMapper,
                                               PermissionMapper permissionMapper) {
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public AuthorizationSnapshot loadSnapshotByRoleIds(List<Long> roleIds) {
        // 认证上下文只关心角色/权限快照结果，因此直接在 identity 仓储里完成映射和聚合。
        // The identity context only needs the authorization snapshot, so the identity repository aggregates it directly.
        if (CollectionUtils.isEmpty(roleIds)) {
            return new AuthorizationSnapshot(List.of(), List.of());
        }

        List<String> roles = roleMapper.selectByIds(roleIds).stream()
                .map(RolePO::getCode)
                .toList();
        List<Long> permissionIds = rolePermissionMapper.findByRoleIds(roleIds).stream()
                .map(RolePermissionPO::getPermissionId)
                .toList();
        List<String> permissions = CollectionUtils.isEmpty(permissionIds)
                ? List.of()
                : permissionMapper.selectByIds(permissionIds).stream()
                .map(PermissionPO::getCode)
                .toList();
        return new AuthorizationSnapshot(roles, permissions);
    }
}
