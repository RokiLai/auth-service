package com.example.authcenter.rpc.dto.request;

import java.io.Serial;
import java.io.Serializable;

public record RegisterRpcRequest(
        String username,
        String password,
        String email
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
