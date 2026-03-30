package com.example.authservice.infra.authorization.repository;

import java.util.Collections;
import java.util.List;

import com.example.authservice.domain.authorization.model.Role;
import com.example.authservice.domain.authorization.repository.RoleRepository;
import com.example.authservice.infra.authorization.mapper.RolePermissionMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import com.example.authservice.infra.authorization.converter.RoleConverter;
import com.example.authservice.infra.authorization.mapper.RoleMapper;
import com.example.authservice.infra.authorization.po.RolePO;

@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleMapper roleMapper;

    private final RolePermissionMapper rolePermissionMapper;

    public RoleRepositoryImpl(RoleMapper roleMapper, RolePermissionMapper rolePermissionMapper) {
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override
    public List<Role> findAll() {
        List<RolePO> rolePOs = roleMapper.selectAll();
        if (CollectionUtils.isEmpty(rolePOs)) {
            return Collections.emptyList();
        }
        return RoleConverter.convertList(rolePOs);
    }

    @Override
    public List<Role> findByIds(List<Long> roleIds) {
        List<RolePO> rolePOs = roleMapper.selectByIds(roleIds);
        if (CollectionUtils.isEmpty(rolePOs)) {
            return Collections.emptyList();
        }
        return RoleConverter.convertList(rolePOs);
    }

    @Override
    public Role findById(Long roleId) {
        RolePO rolePO = roleMapper.selectById(roleId);
        if (rolePO == null) {
            return null;
        }
        return RoleConverter.toEntity(rolePO);
    }

    @Override
    public List<String> findCodesByIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return roleMapper.selectByIds(roleIds).stream().map(RolePO::getCode).toList();
    }

    @Override
    public void save(Role role) {
        rolePermissionMapper.deleteByRoleId(role.getId());
        rolePermissionMapper.insertRolePermissions(role.getId(), role.permissionIds());
    }
}
