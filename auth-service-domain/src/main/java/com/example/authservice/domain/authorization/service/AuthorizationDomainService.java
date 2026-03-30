package com.example.authservice.domain.authorization.service;

import com.example.authservice.domain.identity.model.valueobject.AuthorizationSnapshot;

import java.util.List;
import java.util.Set;

public interface AuthorizationDomainService {

    void authorizeRole(Long roleId, Set<Long> permissionIds);

    AuthorizationSnapshot buildSnapshot(List<Long> roleIds);
}
