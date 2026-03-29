package com.example.authservice.identity.query;

import java.util.List;

public record CurrentIdentity(
        Long id,
        String username,
        String sessionId,
        String token,
        List<String> roles,
        List<String> permissions
) {
}
