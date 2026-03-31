package com.example.authcenter.domain.identity.model.valueobject;

public record TokenClaims(
        Long userId,
        String username,
        String sessionId,
        String rawToken
) {
}
