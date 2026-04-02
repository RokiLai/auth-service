package com.example.authcenter.domain.identity.service.impl;

import com.example.authcenter.domain.identity.model.context.PasswordChangeDecision;
import com.example.authcenter.domain.identity.model.entity.IdentityAccount;
import com.example.authcenter.domain.identity.model.entity.IdentityAccountFactory;
import com.example.authcenter.domain.identity.model.valueobject.PasswordHash;
import com.example.authcenter.domain.identity.model.valueobject.RawPassword;
import com.example.authcenter.domain.identity.repository.IdentityAccountRepository;
import com.example.authcenter.domain.identity.repository.IdentitySessionRepository;
import com.example.authcenter.domain.identity.service.CredentialDomainService;
import com.example.authcenter.domain.identity.service.PasswordHasher;
import com.example.authcenter.exception.auth.OldPasswordIncorrectException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = CredentialDomainServiceImplTest.TestApplication.class,
        properties = {
                "roki.exception.error-code.project-code=10",
                "roki.exception.error-code.default-biz-code=01",
                "roki.exception.error-code.biz-codes.auth=01"
        }
)
class CredentialDomainServiceImplTest {

    @Autowired
    private CredentialDomainService credentialDomainService;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private IdentityAccountFactory identityAccountFactory;

    @MockitoBean
    private IdentityAccountRepository identityAccountRepository;

    @MockitoBean
    private IdentitySessionRepository identitySessionRepository;

    @Test
    void changePasswordShouldReturnRevocationPlanForCurrentSession() {
        IdentityAccount account = identityAccountFactory.restore(
                1L,
                "tester",
                passwordHasher.encode(new RawPassword("123456")),
                "tester@example.com"
        );
        when(identityAccountRepository.findByUsername("tester")).thenReturn(account);
        when(identitySessionRepository.findSessionIdByAccountId(1L)).thenReturn("session-1");

        PasswordChangeDecision decision = credentialDomainService.changePassword(
                1L,
                "tester",
                "session-1",
                "123456",
                "654321"
        );

        assertThat(decision.account().matchPassword(new RawPassword("654321"), passwordHasher)).isTrue();
        assertThat(decision.sessionRevocationPlan().sessionIdToDelete()).isEqualTo("session-1");
        assertThat(decision.sessionRevocationPlan().accountIdBindingToDelete()).isEqualTo(1L);
    }

    @Test
    void changePasswordShouldRejectWrongOldPassword() {
        IdentityAccount account = identityAccountFactory.restore(
                1L,
                "tester",
                passwordHasher.encode(new RawPassword("123456")),
                "tester@example.com"
        );
        when(identityAccountRepository.findByUsername("tester")).thenReturn(account);

        assertThatThrownBy(() -> credentialDomainService.changePassword(
                1L,
                "tester",
                "session-1",
                "wrong-password",
                "654321"
        )).isInstanceOf(OldPasswordIncorrectException.class);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            CredentialDomainServiceImpl.class,
            IdentityAccountFactory.class
    })
    static class TestApplication {
        @Bean
        PasswordHasher passwordHasher() {
            return new TestPasswordHasher();
        }
    }

    private static class TestPasswordHasher implements PasswordHasher {
        @Override
        public PasswordHash encode(RawPassword rawPassword) {
            return new PasswordHash("encoded:" + rawPassword.value());
        }

        @Override
        public boolean matches(RawPassword rawPassword, PasswordHash passwordHash) {
            return ("encoded:" + rawPassword.value()).equals(passwordHash.value());
        }
    }
}
