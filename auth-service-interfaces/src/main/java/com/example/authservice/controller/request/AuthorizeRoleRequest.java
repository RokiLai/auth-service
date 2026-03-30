package com.example.authservice.controller.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AuthorizeRoleRequest(
        @NotNull(message = "角色ID不能为空")
        Long roleId,

        @NotEmpty(message = "权限ID列表不能为空")
        Set<@NotNull(message = "权限ID不能为空") Long> permissionIds
) {
}
