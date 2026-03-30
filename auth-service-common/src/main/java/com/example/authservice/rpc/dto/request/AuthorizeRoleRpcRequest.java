package com.example.authservice.rpc.dto.request;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

public record AuthorizeRoleRpcRequest(
        String token,
        Long roleId,
        Set<Long> permissionIds
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
