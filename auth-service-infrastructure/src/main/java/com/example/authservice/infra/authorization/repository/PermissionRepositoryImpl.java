package com.example.authservice.infra.authorization.repository;

import com.example.authservice.domain.authorization.repository.PermissionRepository;
import com.example.authservice.infra.authorization.mapper.PermissionMapper;
import com.example.authservice.infra.authorization.po.PermissionPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Repository
public class PermissionRepositoryImpl implements PermissionRepository {
    private final PermissionMapper permissionMapper;

    public PermissionRepositoryImpl(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    @Override
    public List<String> selectCodeByIds(List<Long> permissionIds) {
        if (CollectionUtils.isEmpty(permissionIds)) {

            return List.of();
        }
        return permissionMapper.selectByIds(permissionIds).stream().map(PermissionPO::getCode).toList();
    }
}
