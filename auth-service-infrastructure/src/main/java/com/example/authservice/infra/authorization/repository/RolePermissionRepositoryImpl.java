package com.example.authservice.infra.authorization.repository;

import com.example.authservice.domain.authorization.repository.RolePermissionRepository;
import com.example.authservice.infra.authorization.mapper.RolePermissionMapper;
import com.example.authservice.infra.authorization.po.RolePermissionPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Repository
public class RolePermissionRepositoryImpl implements RolePermissionRepository {

    private final RolePermissionMapper rolePermissionMapper;

    public RolePermissionRepositoryImpl(RolePermissionMapper rolePermissionMapper) {
        this.rolePermissionMapper = rolePermissionMapper;
    }


    @Override
    public List<Long> findPermissionIdsByRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return rolePermissionMapper.findByRoleIds(roleIds).stream().map(RolePermissionPO::getPermissionId).toList();
    }

}
