package com.example.authservice.infra.identity.repository;

import com.example.authservice.domain.identity.model.entity.IdentityAccount;
import com.example.authservice.domain.identity.model.entity.IdentityAccountFactory;
import com.example.authservice.domain.identity.model.valueobject.PasswordHash;
import com.example.authservice.domain.identity.repository.IdentityAccountRepository;
import com.example.authservice.infra.identity.mapper.AccountMapper;
import com.example.authservice.infra.identity.po.AccountPO;
import org.springframework.stereotype.Repository;

@Repository
public class IdentityAccountRepositoryImpl implements IdentityAccountRepository {

    private final AccountMapper accountMapper;
    private final IdentityAccountFactory identityAccountFactory;

    public IdentityAccountRepositoryImpl(AccountMapper accountMapper,
                                         IdentityAccountFactory identityAccountFactory) {
        this.accountMapper = accountMapper;
        this.identityAccountFactory = identityAccountFactory;
    }

    @Override
    public IdentityAccount findByUsername(String username) {
        AccountPO account = accountMapper.findByUsername(username);
        if (account == null) {
            return null;
        }
        return identityAccountFactory.restore(
                account.getId(),
                account.getUsername(),
                new PasswordHash(account.getPassword()),
                account.getEmail()
        );
    }

    @Override
    public void save(IdentityAccount account) {
        AccountPO po = new AccountPO();
        po.setId(account.getId());
        po.setUsername(account.getUsername());
        po.setPassword(account.getPasswordHash().value());
        po.setEmail(account.getEmail());
        if (po.getId() == null) {
            accountMapper.insert(po);
        } else {
            accountMapper.update(po);
        }
    }
}
