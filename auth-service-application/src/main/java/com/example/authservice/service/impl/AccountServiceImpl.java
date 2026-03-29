package com.example.authservice.service.impl;

import com.example.authservice.application.context.CurrentOperator;
import com.example.authservice.domain.identity.model.valueobject.RawPassword;
import com.example.authservice.domain.identity.repository.IdentitySessionRepository;
import com.example.authservice.domain.identity.service.PasswordHasher;
import com.example.authservice.domain.repo.AccountRepo;
import com.example.authservice.exception.auth.OldPasswordIncorrectException;
import com.example.authservice.exception.auth.TokenInvalidException;
import com.example.authservice.exception.auth.UsernameAlreadyExistsException;
import com.example.authservice.service.AccountService;
import com.example.authservice.service.command.UpdatePasswordCommand;
import com.example.authservice.domain.model.Account;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private IdentitySessionRepository identitySessionRepository;

    @Autowired
    private PasswordHasher passwordHasher;
    
    public AccountServiceImpl(AccountRepo accountRepo,
                              IdentitySessionRepository identitySessionRepository,
                              PasswordHasher passwordHasher) {
        this.accountRepo = accountRepo;
        this.identitySessionRepository = identitySessionRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public boolean register(String username, String password, String email, List<Long> roleIds) {
        Account account = accountRepo.findByUsername(username);
        if (account != null) {
            throw new UsernameAlreadyExistsException();
        }
        account = Account.register(username, new RawPassword(password), email, passwordHasher);
        accountRepo.save(account);

        if (!CollectionUtils.isEmpty(roleIds)) {
            account.setRoleIds(roleIds);
            accountRepo.updateAccountRole(account);
        }
        return true;
    }

    @Override
    public boolean validatePassword(String username, String password) {
        Account account = accountRepo.findByUsername(username);
        return account != null && account.matchPassword(new RawPassword(password), passwordHasher);
    }

    @Override
    public boolean updatePassword(UpdatePasswordCommand command) {
        CurrentOperator operator = command == null ? null : command.operator();
        if (operator == null || !StringUtils.hasText(operator.username()) || !StringUtils.hasText(operator.sessionId())) {
            throw new TokenInvalidException();
        }
        Account account = accountRepo.findByUsername(operator.username());
        if (account == null || !account.matchPassword(new RawPassword(command.oldPassword()), passwordHasher)) {
            throw new OldPasswordIncorrectException();
        }
        account.updatePassword(new RawPassword(command.newPassword()), passwordHasher);
        accountRepo.save(account);
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
