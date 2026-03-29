package com.example.authservice.identity.usecase.impl;

import com.example.authservice.application.context.CurrentOperator;
import com.example.authservice.domain.identity.model.entity.IdentityAccount;
import com.example.authservice.domain.identity.model.valueobject.RawPassword;
import com.example.authservice.domain.identity.repository.IdentityAccountRepository;
import com.example.authservice.domain.identity.repository.IdentitySessionRepository;
import com.example.authservice.domain.identity.service.PasswordHasher;
import com.example.authservice.exception.auth.OldPasswordIncorrectException;
import com.example.authservice.exception.auth.TokenInvalidException;
import com.example.authservice.identity.usecase.UpdatePasswordUseCase;
import com.example.authservice.identity.usecase.command.UpdatePasswordCommand;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
public class UpdatePasswordUseCaseImpl implements UpdatePasswordUseCase {

    private final IdentityAccountRepository identityAccountRepository;
    private final IdentitySessionRepository identitySessionRepository;
    private final PasswordHasher passwordHasher;

    public UpdatePasswordUseCaseImpl(IdentityAccountRepository identityAccountRepository,
                                     IdentitySessionRepository identitySessionRepository,
                                     PasswordHasher passwordHasher) {
        this.identityAccountRepository = identityAccountRepository;
        this.identitySessionRepository = identitySessionRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public boolean updatePassword(UpdatePasswordCommand command) {
        CurrentOperator operator = command == null ? null : command.operator();
        if (operator == null || !StringUtils.hasText(operator.username()) || !StringUtils.hasText(operator.sessionId())) {
            throw new TokenInvalidException();
        }

        IdentityAccount account = identityAccountRepository.findByUsername(operator.username());
        if (account == null || !account.matchPassword(new RawPassword(command.oldPassword()), passwordHasher)) {
            throw new OldPasswordIncorrectException();
        }

        // 修改密码属于身份能力，密码规则与持久化都收敛在 identity 侧。
        // Password changes are handled in the identity use case so credential rules and persistence stay in the identity context.
        account.updatePassword(new RawPassword(command.newPassword()), passwordHasher);
        identityAccountRepository.save(account);

        // 改密后失效当前会话，且只在账号仍绑定该会话时清理 accountId 索引，避免误删新会话。
        // After a password change, invalidate the current session and only clear the account binding when it still points to that session.
        if (operator.id() != null) {
            identitySessionRepository.deleteBySessionId(operator.sessionId());

            String boundSessionId = identitySessionRepository.findSessionIdByAccountId(operator.id());
            if (Objects.equals(boundSessionId, operator.sessionId())) {
                identitySessionRepository.deleteByAccountId(operator.id());
            }
        }
        return true;
    }
}
