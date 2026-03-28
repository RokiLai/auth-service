package com.example.authservice.service.impl;

import com.example.authservice.auth.IdentityContext;
import com.example.authservice.auth.IdentityContextHolder;
import com.example.authservice.domain.identity.model.RawPassword;
import com.example.authservice.domain.identity.repository.IdentitySessionRepository;
import com.example.authservice.domain.identity.service.PasswordHasher;
import com.example.authservice.domain.repo.AccountRepo;
import com.example.authservice.exception.AuthErrorCode;
import com.example.authservice.service.AccountService;
import com.example.authservice.domain.model.Account;
import com.roki.exception.BusinessException;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
            throw new BusinessException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
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
    public boolean updatePassword(String oldPassword, String newPassword) {
        IdentityContext currentAccount = IdentityContextHolder.get();
        if (currentAccount == null || currentAccount.getUsername() == null || currentAccount.getSessionId() == null || currentAccount.getSessionId().isBlank()) {
            throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
        }
        String username = currentAccount.getUsername();
        Account account = accountRepo.findByUsername(username);
        if (account == null || !account.matchPassword(new RawPassword(oldPassword), passwordHasher)) {
            throw new BusinessException(AuthErrorCode.OLD_PASSWORD_INCORRECT);
        }
        account.updatePassword(new RawPassword(newPassword), passwordHasher);
        accountRepo.save(account);
        if (currentAccount.getId() != null) {
            identitySessionRepository.deleteBySessionId(currentAccount.getSessionId());

            String boundSessionId = identitySessionRepository.findSessionIdByAccountId(currentAccount.getId());
            if (Objects.equals(boundSessionId, currentAccount.getSessionId())) {
                identitySessionRepository.deleteByAccountId(currentAccount.getId());
            }
        }
        return true;
    }
}
