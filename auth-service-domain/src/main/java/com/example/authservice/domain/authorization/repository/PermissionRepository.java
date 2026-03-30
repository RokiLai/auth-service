package com.example.authservice.domain.authorization.repository;

import com.example.authservice.domain.authorization.model.Permission;

import java.util.List;

public interface PermissionRepository {

    List<Permission> findByIds(List<Long> permissionIds);

    List<String> findCodesByIds(List<Long> permissionIds);
}
