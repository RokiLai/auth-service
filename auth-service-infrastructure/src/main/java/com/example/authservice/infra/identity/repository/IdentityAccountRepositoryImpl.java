package com.example.authservice.infra.identity.repository;

import com.example.authservice.domain.identity.model.entity.IdentityAccount;
import com.example.authservice.domain.identity.model.entity.IdentityAccountFactory;
import com.example.authservice.domain.identity.model.valueobject.PasswordHash;
import com.example.authservice.domain.identity.repository.IdentityAccountRepository;
import com.example.authservice.infra.mapper.AccountMapper;
import com.example.authservice.infra.mapper.AccountRoleMapper;
import com.example.authservice.infra.po.AccountPO;
import com.example.authservice.infra.po.AccountRolePO;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Repository
public class IdentityAccountRepositoryImpl implements IdentityAccountRepository {

    private final AccountMapper accountMapper;
    private final AccountRoleMapper accountRoleMapper;
    private final IdentityAccountFactory identityAccountFactory;

    public IdentityAccountRepositoryImpl(AccountMapper accountMapper,
                                         AccountRoleMapper accountRoleMapper,
                                         IdentityAccountFactory identityAccountFactory) {
        this.accountMapper = accountMapper;
        this.accountRoleMapper = accountRoleMapper;
        this.identityAccountFactory = identityAccountFactory;
    }

    @Override
    public IdentityAccount findByUsername(String username) {
        AccountPO account = accountMapper.findByUsername(username);
        if (account == null) {
            return null;
        }
        List<AccountRolePO> accountRolePOS = accountRoleMapper.selectByAccountId(account.getId());
        return identityAccountFactory.restore(
                account.getId(),
                account.getUsername(),
                new PasswordHash(account.getPassword()),
                account.getEmail(),
                CollectionUtils.isEmpty(accountRolePOS) ? null : accountRolePOS.stream().map(AccountRolePO::getRoleId).toList()
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
