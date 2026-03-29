package com.example.authservice.domain.identity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IdentitySession implements Serializable {
    private String sessionId;
    private Long accountId;
    private String username;
    private String token;
    private List<String> roles;
    private List<String> permissions;

    /**
     * 基于已认证账号创建一条新的登录会话。
     */
    public static IdentitySession createFor(IdentityAccount account, String sessionId, String token) {
        IdentitySession session = new IdentitySession();
        session.sessionId = sessionId;
        session.accountId = account.getId();
        session.username = account.getUsername();
        session.token = token;
        return session;
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
        this.roles = roles;
        this.permissions = permissions;
    }
}
