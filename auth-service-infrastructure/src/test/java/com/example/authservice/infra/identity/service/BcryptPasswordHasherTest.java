package com.example.authservice.infra.identity.service;

import com.example.authservice.domain.identity.model.valueobject.PasswordHash;
import com.example.authservice.domain.identity.model.valueobject.RawPassword;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BcryptPasswordHasherTest {

    private final BcryptPasswordHasher passwordHasher = new BcryptPasswordHasher();

    @Test
    void shouldEncodeNewPasswordAsBcrypt() {
        PasswordHash encoded = passwordHasher.encode(new RawPassword("123456"));

        assertThat(encoded.value()).startsWith("$2");
        assertThat(passwordHasher.matches(new RawPassword("123456"), encoded)).isTrue();
        assertThat(passwordHasher.matches(new RawPassword("654321"), encoded)).isFalse();
    }

    @Test
    void shouldRejectLegacyPlaintextPasswordValues() {
        PasswordHash legacy = new PasswordHash("123456");

        assertThat(passwordHasher.matches(new RawPassword("123456"), legacy)).isFalse();
    }
}
