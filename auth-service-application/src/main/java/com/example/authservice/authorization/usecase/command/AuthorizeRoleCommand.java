package com.example.authservice.authorization.usecase.command;

import java.util.Set;

/**
 * 角色授权命令负责承接接口层传入的授权意图。
 * The role-authorization command carries the authorization intent from the interface layer.
 */
public record AuthorizeRoleCommand(
        Long roleId,
        Set<Long> permissionIds
) {
}
