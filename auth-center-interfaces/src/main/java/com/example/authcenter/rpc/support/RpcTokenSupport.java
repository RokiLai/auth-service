package com.example.authcenter.rpc.support;

import org.springframework.util.StringUtils;

public final class RpcTokenSupport {

    private static final String BEARER_PREFIX = "Bearer ";

    private RpcTokenSupport() {
    }

    public static String resolveRawToken(String token) {
        if (!StringUtils.hasText(token)) {
            return token;
        }

        String candidate = token.trim();
        if (candidate.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return candidate.substring(BEARER_PREFIX.length()).trim();
        }
        return candidate;
    }
}
