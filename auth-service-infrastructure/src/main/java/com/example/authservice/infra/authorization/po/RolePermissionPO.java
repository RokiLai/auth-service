package com.example.authservice.infra.authorization.po;

import lombok.Data;

@Data
public class RolePermissionPO {
    private Long roleId;
    private Long permissionId;
}
