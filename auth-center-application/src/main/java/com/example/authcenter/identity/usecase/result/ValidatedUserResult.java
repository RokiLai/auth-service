package com.example.authcenter.identity.usecase.result;

public record ValidatedUserResult(
        Long id,
        String username,
        String email
) {
}
