package com.example.authservice.infra.repo;

import com.example.authservice.domain.repo.AccountRoleRepo;
import com.example.authservice.infra.mapper.AccountRoleMapper;
import com.example.authservice.infra.po.AccountRolePO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountRoleRepoImpl implements AccountRoleRepo {
    private final AccountRoleMapper accountRoleMapper;

    public AccountRoleRepoImpl(AccountRoleMapper accountRoleMapper) {
        this.accountRoleMapper = accountRoleMapper;
    }

    @Override
    public List<Long> findRoleIdsByAccountId(Long accountId) {
        List<AccountRolePO> po = accountRoleMapper.selectByAccountId(accountId);
        return po.stream().map(AccountRolePO::getRoleId).toList();
    }
}
