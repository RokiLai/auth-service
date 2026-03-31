package com.example.authcenter.rpc.dto.request;

import java.io.Serial;
import java.io.Serializable;

public record UpdatePasswordRpcRequest(
        String token,
        String oldPassword,
        String newPassword
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
