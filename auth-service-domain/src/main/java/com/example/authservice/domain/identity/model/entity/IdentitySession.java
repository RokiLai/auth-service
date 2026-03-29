package com.example.authservice.domain.identity.model.entity;

import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Getter
public class IdentitySession implements Serializable {
    private final String sessionId;
    private final Long accountId;
    private final String username;
    private final String token;
    private List<String> roles;
    private List<String> permissions;

    IdentitySession(String sessionId,
                    Long accountId,
                    String username,
                    String token,
                    List<String> roles,
                    List<String> permissions) {
        this.sessionId = sessionId;
        this.accountId = accountId;
        this.username = username;
        this.token = token;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }

    /**
     * 鉴权时用会话中记录的 token 校验当前请求携带的 token。
     */
    public boolean matchesToken(String rawToken) {
        return Objects.equals(token, rawToken);
    }

    /**
     * 登录成功后把角色和权限快照挂到当前会话上，供后续鉴权直接读取。
     */
    public void grantAuthorities(List<String> roles, List<String> permissions) {
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }
}
