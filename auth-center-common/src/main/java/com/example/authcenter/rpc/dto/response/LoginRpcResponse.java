package com.example.authcenter.rpc.dto.response;

import java.io.Serial;
import java.io.Serializable;

public record LoginRpcResponse(
        Long id,
        String username,
        String email,
        String token
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
