package com.example.authservice.identity.usecase.result;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResult {
    private Long id;
    private String username;
    private String email;
    private String token;
}
