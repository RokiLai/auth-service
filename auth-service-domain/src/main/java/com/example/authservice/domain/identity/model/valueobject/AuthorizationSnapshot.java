package com.example.authservice.domain.identity.model.valueobject;

import java.util.List;

public record AuthorizationSnapshot(
        List<String> roles,
        List<String> permissions
) {
}
