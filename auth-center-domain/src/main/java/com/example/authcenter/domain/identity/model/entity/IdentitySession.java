package com.example.authcenter.domain.identity.model.entity;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class IdentitySession implements Serializable {
    private final String sessionId;
    private final Long accountId;
    private final String username;

    IdentitySession(String sessionId,
                    Long accountId,
                    String username) {
        this.sessionId = sessionId;
        this.accountId = accountId;
        this.username = username;
    }
}
