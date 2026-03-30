package com.example.authservice.domain.identity.service;

import com.example.authservice.domain.identity.model.context.AuthenticatedIdentity;

public interface AuthenticationDomainService {

    /**
     * 完成登录认证的核心领域规则，并产出新的登录会话。
     */
    AuthenticatedIdentity authenticate(String username, String password);
}
