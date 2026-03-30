package com.example.authservice.domain.authorization.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

    /**
     * 角色授权属于角色聚合内部状态变更，调用方只表达“最新权限集合”。
     */
    public void authorize(Set<Long> newPermissions) {
        Objects.requireNonNull(newPermissions, "权限集合不能为空");
        this.permissionIds.clear();
        this.permissionIds.addAll(new LinkedHashSet<>(newPermissions));
    }

    public List<Long> permissionIds() {
        return List.copyOf(permissionIds);
    }
}
