package com.example.authservice.domain.identity.model;

import lombok.Data;

import java.util.List;

@Data
public class CurrentIdentity {
    private Long id;
    private String username;
    private String sessionId;
    private String token;
    private List<String> roles;
    private List<String> permissions;
}
