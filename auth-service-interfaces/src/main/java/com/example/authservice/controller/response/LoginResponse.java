package com.example.authservice.controller.response;

/**
 * 登录接口的响应对象，仅用于接口层返回。
 * Response model for the login endpoint, used only at the interface layer.
 */
public record LoginResponse(
        Long id,
        String username,
        String email,
        String token
) {
}
