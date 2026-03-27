package com.example.authservice.auth;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class LoginSession implements Serializable {
    private String sessionId;
    private Long accountId;
    private String username;
    private String token;
    private List<String> roles;
    private List<String> permissions;
}
