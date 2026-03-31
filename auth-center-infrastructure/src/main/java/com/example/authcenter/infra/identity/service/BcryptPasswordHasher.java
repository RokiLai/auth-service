package com.example.authcenter.infra.identity.service;

import com.example.authcenter.domain.identity.model.valueobject.PasswordHash;
import com.example.authcenter.domain.identity.model.valueobject.RawPassword;
import com.example.authcenter.domain.identity.service.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

    @Override
    public PasswordHash encode(RawPassword rawPassword) {
        return new PasswordHash(delegate.encode(rawPassword.value()));
    }

    @Override
    public boolean matches(RawPassword rawPassword, PasswordHash passwordHash) {
        if (rawPassword == null || passwordHash == null || passwordHash.value() == null) {
            return false;
        }
        return delegate.matches(rawPassword.value(), passwordHash.value());
    }
}
