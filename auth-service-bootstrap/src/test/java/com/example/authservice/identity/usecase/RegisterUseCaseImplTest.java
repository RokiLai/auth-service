package com.example.authservice.identity.usecase;

import com.example.authservice.domain.identity.model.entity.IdentityAccount;
import com.example.authservice.domain.identity.model.valueobject.RawPassword;
import com.example.authservice.domain.identity.repository.IdentityAccountRepository;
import com.example.authservice.domain.identity.service.PasswordHasher;
import com.example.authservice.identity.usecase.command.RegisterCommand;
import com.example.authservice.identity.usecase.impl.RegisterUseCaseImpl;
import com.example.authservice.infra.identity.service.BcryptPasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterUseCaseImplTest {

    private final IdentityAccountRepository identityAccountRepository = mock(IdentityAccountRepository.class);
    private final PasswordHasher passwordHasher = new BcryptPasswordHasher();

    private RegisterUseCase registerUseCase;

    @BeforeEach
    void setUp() {
        registerUseCase = new RegisterUseCaseImpl(identityAccountRepository, passwordHasher);
    }

    @Test
    void registerShouldCreateIdentityAccountAndPersistIt() {
        when(identityAccountRepository.findByUsername("new-user")).thenReturn(null);
        doAnswer(invocation -> null).when(identityAccountRepository).save(any(IdentityAccount.class));

        boolean registered = registerUseCase.register(new RegisterCommand(
                "new-user",
                "123456",
                "new-user@example.com"
        ));

        assertThat(registered).isTrue();
        verify(identityAccountRepository).save(any(IdentityAccount.class));
    }
}
