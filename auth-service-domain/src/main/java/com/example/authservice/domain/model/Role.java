package com.example.authservice.domain.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public class Role {
    private final Long id;
    private final String code;
    private final List<Long> permissionIds;

    private Role(Long id, String code, List<Long> permissionIds) {
        this.id = id;
        this.code = code;
        this.permissionIds = permissionIds == null ? new ArrayList<>() : new ArrayList<>(permissionIds);
    }

    public static Role restore(Long id, String code, List<Long> permissionIds) {
        return new Role(id, code, permissionIds);
    }

    public void addPermissions(List<Long> newPermissions) {
        Objects.requireNonNull(newPermissions, "权限集合不能为空");
        this.permissionIds.clear();
        this.permissionIds.addAll(newPermissions);
    }

    // 领域行为：添加单个权限
    public void addPermission(Long permissionId) {
        Objects.requireNonNull(permissionId, "权限不能为空");
        this.permissionIds.add(permissionId);
    }
}
