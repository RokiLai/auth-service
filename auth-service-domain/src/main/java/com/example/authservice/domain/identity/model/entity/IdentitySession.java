package com.example.authservice.domain.identity.model.entity;

import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

@Getter
public class IdentitySession implements Serializable {
    private final String sessionId;
    private final Long accountId;
    private final String username;
    private final String token;

    IdentitySession(String sessionId,
                    Long accountId,
                    String username,
                    String token) {
        this.sessionId = sessionId;
        this.accountId = accountId;
        this.username = username;
        this.token = token;
    }

    /**
     * 鉴权时用会话中记录的 token 校验当前请求携带的 token。
     */
    public boolean matchesToken(String rawToken) {
        return Objects.equals(token, rawToken);
    }
}
