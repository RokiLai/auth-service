package com.example.authservice.identity.usecase.impl;

import com.example.authservice.domain.identity.model.entity.IdentityAccount;
import com.example.authservice.domain.identity.model.entity.IdentityAccountFactory;
import com.example.authservice.domain.identity.model.valueobject.RawPassword;
import com.example.authservice.domain.identity.repository.IdentityAccountRepository;
import com.example.authservice.domain.identity.service.PasswordHasher;
import com.example.authservice.exception.auth.UsernameAlreadyExistsException;
import com.example.authservice.identity.usecase.RegisterUseCase;
import com.example.authservice.identity.usecase.command.RegisterCommand;
import org.springframework.stereotype.Service;

@Service
public class RegisterUseCaseImpl implements RegisterUseCase {

    private final IdentityAccountRepository identityAccountRepository;
    private final PasswordHasher passwordHasher;
    private final IdentityAccountFactory identityAccountFactory;

    public RegisterUseCaseImpl(IdentityAccountRepository identityAccountRepository,
                               PasswordHasher passwordHasher,
                               IdentityAccountFactory identityAccountFactory) {
        this.identityAccountRepository = identityAccountRepository;
        this.passwordHasher = passwordHasher;
        this.identityAccountFactory = identityAccountFactory;
    }

    @Override
    public boolean register(RegisterCommand command) {
        if (identityAccountRepository.findByUsername(command.username()) != null) {
            throw new UsernameAlreadyExistsException();
        }

        // 注册属于身份生命周期能力，直接在 identity 上下文内创建并持久化账号。
        // Registration is part of the identity lifecycle, so the account is created and persisted directly in the identity context.
        IdentityAccount account = identityAccountFactory.register(
                command.username(),
                new RawPassword(command.password()),
                command.email(),
                passwordHasher
        );
        identityAccountRepository.save(account);
        return true;
    }
}
