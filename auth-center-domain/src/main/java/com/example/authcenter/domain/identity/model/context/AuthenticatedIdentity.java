package com.example.authcenter.domain.identity.model.context;

import com.example.authcenter.domain.identity.model.entity.IdentityAccount;
import com.example.authcenter.domain.identity.model.entity.IdentitySession;

public record AuthenticatedIdentity(
        IdentityAccount account,
        IdentitySession session,
        String token
) {
}
