package com.example.authservice.auth;

import lombok.Data;

import java.util.List;

@Data
public class IdentityContext {
    private Long id;
    private String username;
    private String sessionId;
    private String token;
    private List<String> roles;
    private List<String> permissions;
}
