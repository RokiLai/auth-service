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
    // 角色聚合内部维护权限关联，仓储负责把该状态持久化到关系表。
    // The role aggregate owns the permission association state, and the repository persists it into relation tables.
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
     * Role authorization is an internal aggregate state change; callers only provide the latest permission set.
     */
    public void authorize(Set<Long> newPermissions) {
        Objects.requireNonNull(newPermissions, "权限集合不能为空");
        this.permissionIds.clear();
        this.permissionIds.addAll(new LinkedHashSet<>(newPermissions));
    }

    /**
     * 对外暴露只读权限视图，避免调用方绕过聚合直接改内部集合。
     * Exposes a read-only permission view so callers cannot mutate aggregate state directly.
     */
    public List<Long> permissionIds() {
        return List.copyOf(permissionIds);
    }
}
