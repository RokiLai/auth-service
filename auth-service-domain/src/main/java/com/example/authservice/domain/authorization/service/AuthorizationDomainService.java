package com.example.authservice.domain.authorization.service;

import com.example.authservice.domain.identity.model.valueobject.AuthorizationSnapshot;

import java.util.List;
import java.util.Set;

/**
 * 授权域服务负责承接跨聚合的授权规则与授权快照组装。
 * The authorization domain service encapsulates cross-aggregate authorization rules and snapshot assembly.
 */
public interface AuthorizationDomainService {

    /**
     * 为角色重新分配一组最新权限。
     * Reassigns the latest permission set for a role.
     */
    void authorizeRole(Long roleId, Set<Long> permissionIds);

    /**
     * 根据角色集合构建登录态需要的授权快照。
     * Builds the authorization snapshot required by an authenticated session from role ids.
     */
    AuthorizationSnapshot buildSnapshot(List<Long> roleIds);
}
