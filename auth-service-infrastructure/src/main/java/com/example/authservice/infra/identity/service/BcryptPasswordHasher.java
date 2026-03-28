package com.example.authservice.infra.identity.service;

import com.example.authservice.domain.identity.model.PasswordHash;
import com.example.authservice.domain.identity.model.RawPassword;
import com.example.authservice.domain.identity.service.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

    @Override
    public PasswordHash encode(RawPassword rawPassword) {
        return new PasswordHash(delegate.encode(rawPassword.getValue()));
    }

    @Override
    public boolean matches(RawPassword rawPassword, PasswordHash passwordHash) {
        if (rawPassword == null || passwordHash == null || passwordHash.getValue() == null) {
            return false;
        }
        return delegate.matches(rawPassword.getValue(), passwordHash.getValue());
    }
}
