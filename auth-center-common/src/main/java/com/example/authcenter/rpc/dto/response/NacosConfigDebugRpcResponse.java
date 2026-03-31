package com.example.authcenter.rpc.dto.response;

import java.io.Serial;
import java.io.Serializable;

public record NacosConfigDebugRpcResponse(
        String applicationName,
        String activeProfile,
        String demoMessage,
        String sampleFlag
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
