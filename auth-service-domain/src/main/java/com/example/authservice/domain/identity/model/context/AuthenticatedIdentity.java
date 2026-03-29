package com.example.authservice.domain.identity.model.context;

import com.example.authservice.domain.identity.model.entity.IdentityAccount;
import com.example.authservice.domain.identity.model.entity.IdentitySession;

public record AuthenticatedIdentity(
        IdentityAccount account,
        IdentitySession session
) {
}
