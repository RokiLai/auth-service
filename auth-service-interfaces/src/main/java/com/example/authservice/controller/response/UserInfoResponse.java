package com.example.authservice.controller.response;

public record UserInfoResponse(
        Long id,
        String username,
        String email
) {
}
