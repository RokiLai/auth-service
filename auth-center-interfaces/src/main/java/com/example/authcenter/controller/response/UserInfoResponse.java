package com.example.authcenter.controller.response;

public record UserInfoResponse(
        Long id,
        String username,
        String email
) {
}
