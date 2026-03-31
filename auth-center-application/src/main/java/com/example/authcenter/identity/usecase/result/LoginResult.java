package com.example.authcenter.identity.usecase.result;

public record LoginResult(
        Long id,
        String username,
        String email,
        String token
) {
}
