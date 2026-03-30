package com.example.authservice.authorization.usecase;

import java.util.Set;

public interface AuthorizeRoleUseCase {
    /**
     * 批量授权
     * @param roleId
     * @param permissionIds
     */
    void batchAuthorize(Long roleId, Set<Long> permissionIds);
}
