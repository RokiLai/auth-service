package com.example.authcenter.rpc.dto.request;

import java.io.Serial;
import java.io.Serializable;

public record LogoutRpcRequest(
        String token
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
