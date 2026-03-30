package com.example.authservice.infra.repo;

import com.example.authservice.domain.authorization.repository.RolePermissionRepository;
import com.example.authservice.infra.mapper.RolePermissionMapper;
import com.example.authservice.infra.po.RolePermissionPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Repository
public class RolePermissionRepoImpl implements RolePermissionRepository {

    private final RolePermissionMapper rolePermissionMapper;

    public RolePermissionRepoImpl(RolePermissionMapper rolePermissionMapper) {
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
