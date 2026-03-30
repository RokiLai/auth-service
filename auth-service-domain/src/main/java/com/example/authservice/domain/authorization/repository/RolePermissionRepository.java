package com.example.authservice.domain.authorization.repository;

import java.util.List;

public interface RolePermissionRepository {

    /**
     * 根据角色ID列表查询权限ID列表
     *
     * @param roleIds 角色ID列表
     * @return 权限ID列表
     */
    List<Long> findPermissionIdsByRoleIds(List<Long> roleIds);


}
