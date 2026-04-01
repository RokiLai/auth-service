package com.example.authcenter.identity.usecase;

import com.example.authcenter.application.context.CurrentOperator;
import com.example.authcenter.domain.identity.model.entity.IdentityAccount;
import com.example.authcenter.domain.identity.model.entity.IdentityAccountFactory;
import com.example.authcenter.domain.identity.model.valueobject.RawPassword;
import com.example.authcenter.domain.identity.repository.IdentityAccountRepository;
import com.example.authcenter.identity.usecase.impl.ValidateTokenUseCaseImpl;
import com.example.authcenter.identity.usecase.result.ValidatedUserResult;
import com.example.authcenter.infra.identity.service.BcryptPasswordHasher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidateTokenUseCaseImplTest {

    private final AuthenticateUseCase authenticateUseCase = mock(AuthenticateUseCase.class);
    private final IdentityAccountRepository identityAccountRepository = mock(IdentityAccountRepository.class);
    private final IdentityAccountFactory identityAccountFactory = new IdentityAccountFactory();
    private final BcryptPasswordHasher passwordHasher = new BcryptPasswordHasher();

    private final ValidateTokenUseCase validateTokenUseCase =
            new ValidateTokenUseCaseImpl(authenticateUseCase, identityAccountRepository);

    @Test
    void validateShouldReturnBasicUserInfoOfAuthenticatedToken() {
        String token = "token-value";
        when(authenticateUseCase.authenticate(token))
                .thenReturn(new CurrentOperator(1L, "tester", "session-1", token));

        IdentityAccount account = identityAccountFactory.restore(
                1L,
                "tester",
                passwordHasher.encode(new RawPassword("123456")),
                "tester@example.com"
        );
        when(identityAccountRepository.findById(1L)).thenReturn(account);

        ValidatedUserResult result = validateTokenUseCase.validate(token);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo("tester");
        assertThat(result.email()).isEqualTo("tester@example.com");
    }
}
