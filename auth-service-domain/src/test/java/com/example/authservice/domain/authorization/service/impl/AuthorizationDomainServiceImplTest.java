package com.example.authservice.domain.authorization.service.impl;

import com.example.authservice.domain.authorization.model.Permission;
import com.example.authservice.domain.authorization.model.Role;
import com.example.authservice.domain.authorization.repository.PermissionRepository;
import com.example.authservice.domain.authorization.repository.RolePermissionRepository;
import com.example.authservice.domain.authorization.repository.RoleRepository;
import com.example.authservice.domain.identity.model.valueobject.AuthorizationSnapshot;
import com.example.authservice.exception.auth.RoleAuthorizeParamInvalidException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = AuthorizationDomainServiceImplTest.TestApplication.class,
        properties = {
                "roki.exception.error-code.project-code=10",
                "roki.exception.error-code.default-biz-code=01",
                "roki.exception.error-code.biz-codes.auth=01"
        }
)
class AuthorizationDomainServiceImplTest {

    @Autowired
    private AuthorizationDomainServiceImpl authorizationDomainService;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private PermissionRepository permissionRepository;

    @MockBean
    private RolePermissionRepository rolePermissionRepository;

    @Test
    void shouldAuthorizeRoleByReplacingPermissionsOnAggregate() {
        Role role = Role.restore(1L, "admin", List.of(9L));
        when(roleRepository.findById(1L)).thenReturn(role);
        when(permissionRepository.findByIds(anyList())).thenReturn(List.of(
                new Permission(2L, "user:create"),
                new Permission(3L, "user:delete")
        ));

        authorizationDomainService.authorizeRole(1L, Set.of(2L, 3L));

        assertThat(role.permissionIds()).containsExactlyInAnyOrder(2L, 3L);
        verify(roleRepository).save(role);
    }

    @Test
    void shouldRejectUnknownPermissionIdsWhenAuthorizingRole() {
        Role role = Role.restore(1L, "admin", List.of());
        when(roleRepository.findById(1L)).thenReturn(role);
        when(permissionRepository.findByIds(anyList())).thenReturn(List.of(
                new Permission(2L, "user:create")
        ));

        assertThatThrownBy(() -> authorizationDomainService.authorizeRole(1L, Set.of(2L, 3L)))
                .isInstanceOf(RoleAuthorizeParamInvalidException.class);
    }

    @Test
    void shouldBuildAuthorizationSnapshotFromAuthorizationRepositories() {
        when(roleRepository.findCodesByIds(List.of(1L, 2L))).thenReturn(List.of("admin", "auditor"));
        when(rolePermissionRepository.findPermissionIdsByRoleIds(List.of(1L, 2L))).thenReturn(List.of(7L, 8L));
        when(permissionRepository.findCodesByIds(List.of(7L, 8L))).thenReturn(List.of("user:read", "user:write"));

        AuthorizationSnapshot snapshot = authorizationDomainService.buildSnapshot(List.of(1L, 2L));

        assertThat(snapshot.roles()).containsExactly("admin", "auditor");
        assertThat(snapshot.permissions()).containsExactly("user:read", "user:write");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(AuthorizationDomainServiceImpl.class)
    static class TestApplication {
    }
}
