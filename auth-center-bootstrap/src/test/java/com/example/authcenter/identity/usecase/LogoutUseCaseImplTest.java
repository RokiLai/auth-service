package com.example.authcenter.identity.usecase;

import com.example.authcenter.application.context.CurrentOperator;
import com.example.authcenter.domain.identity.repository.IdentitySessionRepository;
import com.example.authcenter.domain.identity.service.SessionDomainService;
import com.example.authcenter.domain.identity.service.impl.SessionDomainServiceImpl;
import com.example.authcenter.identity.usecase.command.LogoutCommand;
import com.example.authcenter.identity.usecase.impl.LogoutUseCaseImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogoutUseCaseImplTest {

    private final IdentitySessionRepository identitySessionRepository = mock(IdentitySessionRepository.class);
    private SessionDomainService sessionDomainService;

    private LogoutUseCase logoutUseCase;

    @BeforeEach
    void setUp() {
        sessionDomainService = new SessionDomainServiceImpl(identitySessionRepository);
        logoutUseCase = new LogoutUseCaseImpl(identitySessionRepository, sessionDomainService);
    }

    @Test
    void logoutShouldRevokeCurrentSessionAndBinding() {
        when(identitySessionRepository.findSessionIdByAccountId(1L)).thenReturn("session-1");

        boolean loggedOut = logoutUseCase.logout(new LogoutCommand(
                new CurrentOperator(1L, "tester", "session-1")
        ));

        assertThat(loggedOut).isTrue();
        verify(identitySessionRepository).deleteBySessionId("session-1");
        verify(identitySessionRepository).deleteByAccountId(1L);
    }
}
