package com.example.authservice.infra.authorization.repository;

import com.example.authservice.domain.authorization.repository.AccountRoleRepository;
import com.example.authservice.infra.authorization.mapper.AccountRoleMapper;
import com.example.authservice.infra.authorization.po.AccountRolePO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountRoleRepositoryImpl implements AccountRoleRepository {
    private final AccountRoleMapper accountRoleMapper;

    public AccountRoleRepositoryImpl(AccountRoleMapper accountRoleMapper) {
        this.accountRoleMapper = accountRoleMapper;
    }

    @Override
    public List<Long> findRoleIdsByAccountId(Long accountId) {
        List<AccountRolePO> po = accountRoleMapper.selectByAccountId(accountId);
        return po.stream().map(AccountRolePO::getRoleId).toList();
    }
}
