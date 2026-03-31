package com.example.authcenter.domain.identity.service;

import com.example.authcenter.domain.identity.model.valueobject.PasswordHash;
import com.example.authcenter.domain.identity.model.valueobject.RawPassword;

public interface PasswordHasher {

    PasswordHash encode(RawPassword rawPassword);

    boolean matches(RawPassword rawPassword, PasswordHash passwordHash);
}
