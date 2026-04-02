package com.example.authcenter.domain.identity.service.impl;

import com.example.authcenter.domain.identity.model.entity.IdentityAccount;
import com.example.authcenter.domain.identity.model.entity.IdentityAccountFactory;
import com.example.authcenter.domain.identity.model.valueobject.PasswordHash;
import com.example.authcenter.domain.identity.model.valueobject.RawPassword;
import com.example.authcenter.domain.identity.repository.IdentityAccountRepository;
import com.example.authcenter.domain.identity.service.PasswordHasher;
import com.example.authcenter.domain.identity.service.RegistrationDomainService;
import com.example.authcenter.exception.auth.UsernameAlreadyExistsException;
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
        classes = RegistrationDomainServiceImplTest.TestApplication.class,
        properties = {
                "roki.exception.error-code.project-code=10",
                "roki.exception.error-code.default-biz-code=01",
                "roki.exception.error-code.biz-codes.auth=01"
        }
)
class RegistrationDomainServiceImplTest {

    @Autowired
    private RegistrationDomainService registrationDomainService;

    @Autowired
    private PasswordHasher passwordHasher;

    @MockitoBean
    private IdentityAccountRepository identityAccountRepository;

    @Test
    void registerShouldCreateAccountWhenUsernameIsAvailable() {
        when(identityAccountRepository.existsByUsername("new-user")).thenReturn(false);

        IdentityAccount account = registrationDomainService.register("new-user", "123456", "new-user@example.com");

        assertThat(account.getUsername()).isEqualTo("new-user");
        assertThat(account.getEmail()).isEqualTo("new-user@example.com");
        assertThat(account.matchPassword(new RawPassword("123456"), passwordHasher)).isTrue();
    }

    @Test
    void registerShouldRejectDuplicatedUsername() {
        when(identityAccountRepository.existsByUsername("new-user")).thenReturn(true);

        assertThatThrownBy(() -> registrationDomainService.register("new-user", "123456", "new-user@example.com"))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            RegistrationDomainServiceImpl.class,
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
