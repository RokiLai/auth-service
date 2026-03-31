package com.example.authservice.identity.usecase;

import com.example.authservice.application.context.CurrentOperator;
import com.example.authservice.domain.identity.model.entity.IdentityAccount;
import com.example.authservice.domain.identity.model.entity.IdentityAccountFactory;
import com.example.authservice.domain.identity.model.valueobject.RawPassword;
import com.example.authservice.domain.identity.repository.IdentityAccountRepository;
import com.example.authservice.domain.identity.repository.IdentitySessionRepository;
import com.example.authservice.domain.identity.service.PasswordHasher;
import com.example.authservice.exception.auth.OldPasswordIncorrectException;
import com.example.authservice.identity.usecase.command.UpdatePasswordCommand;
import com.example.authservice.identity.usecase.impl.UpdatePasswordUseCaseImpl;
import com.example.authservice.infra.identity.service.BcryptPasswordHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = UpdatePasswordUseCaseImplTest.TestApplication.class,
        properties = {
                "roki.exception.error-code.project-code=10",
                "roki.exception.error-code.default-biz-code=01",
                "roki.exception.error-code.biz-codes.auth=01"
        }
)
@ActiveProfiles("test")
class UpdatePasswordUseCaseImplTest {

    @Autowired
    private UpdatePasswordUseCase updatePasswordUseCase;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private IdentityAccountFactory identityAccountFactory;

    @MockBean
    private IdentityAccountRepository identityAccountRepository;

    @MockBean
    private IdentitySessionRepository identitySessionRepository;

    @AfterEach
    void tearDown() {
        reset(identityAccountRepository, identitySessionRepository);
    }

    @Test
    void updatePasswordShouldPersistNewHashAndInvalidateCurrentSession() {
        IdentityAccount account = identityAccountFactory.restore(
                1L,
                "tester",
                passwordHasher.encode(new RawPassword("123456")),
                "tester@example.com"
        );
        when(identityAccountRepository.findByUsername("tester")).thenReturn(account);
        when(identitySessionRepository.findSessionIdByAccountId(1L)).thenReturn("session-1");

        boolean updated = updatePasswordUseCase.updatePassword(new UpdatePasswordCommand(
                new CurrentOperator(1L, "tester", "session-1", null),
                "123456",
                "654321"
        ));

        assertThat(updated).isTrue();
        assertThat(account.matchPassword(new RawPassword("654321"), passwordHasher)).isTrue();
        verify(identityAccountRepository).save(account);
        verify(identitySessionRepository).deleteBySessionId("session-1");
        verify(identitySessionRepository).deleteByAccountId(1L);
    }

    @Test
    void updatePasswordShouldRejectWrongOldPassword() {
        IdentityAccount account = identityAccountFactory.restore(
                1L,
                "tester",
                passwordHasher.encode(new RawPassword("123456")),
                "tester@example.com"
        );
        when(identityAccountRepository.findByUsername("tester")).thenReturn(account);

        assertThatThrownBy(() -> updatePasswordUseCase.updatePassword(new UpdatePasswordCommand(
                new CurrentOperator(1L, "tester", "session-1", null),
                "wrong-password",
                "654321"
        ))).isInstanceOf(OldPasswordIncorrectException.class);

        verifyNoMoreInteractions(identitySessionRepository);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            UpdatePasswordUseCaseImpl.class,
            IdentityAccountFactory.class,
            BcryptPasswordHasher.class
    })
    static class TestApplication {
    }
}
