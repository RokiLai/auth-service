package com.example.authservice.domain.identity.repository;

import com.example.authservice.domain.identity.model.entity.IdentityAccount;

public interface IdentityAccountRepository {

    IdentityAccount findByUsername(String username);

    void save(IdentityAccount account);
}
