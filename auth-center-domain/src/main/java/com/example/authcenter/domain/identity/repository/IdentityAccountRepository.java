package com.example.authcenter.domain.identity.repository;

import com.example.authcenter.domain.identity.model.entity.IdentityAccount;

public interface IdentityAccountRepository {

    boolean existsByUsername(String username);

    IdentityAccount findById(Long id);

    IdentityAccount findByUsername(String username);

    void save(IdentityAccount account);
}
