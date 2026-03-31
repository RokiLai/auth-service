package com.example.authcenter.identity.usecase;

import com.example.authcenter.domain.identity.model.entity.IdentityAccount;
import com.example.authcenter.domain.identity.model.entity.IdentityAccountFactory;
import com.example.authcenter.domain.identity.model.valueobject.RawPassword;
import com.example.authcenter.domain.identity.repository.IdentityAccountRepository;
import com.example.authcenter.domain.identity.service.PasswordHasher;
import com.example.authcenter.identity.usecase.command.RegisterCommand;
import com.example.authcenter.identity.usecase.impl.RegisterUseCaseImpl;
import com.example.authcenter.infra.identity.service.BcryptPasswordHasher;
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
    private final IdentityAccountFactory identityAccountFactory = new IdentityAccountFactory();

    private RegisterUseCase registerUseCase;

    @BeforeEach
    void setUp() {
        registerUseCase = new RegisterUseCaseImpl(identityAccountRepository, passwordHasher, identityAccountFactory);
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
