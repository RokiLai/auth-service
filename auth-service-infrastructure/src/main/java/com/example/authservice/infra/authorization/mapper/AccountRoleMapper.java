package com.example.authservice.infra.authorization.mapper;

import com.example.authservice.infra.authorization.po.AccountRolePO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AccountRoleMapper {

    /**
     * 根据账户ID删除角色关联
     * @param accountId 账户ID
     */
    void deleteByAccountId(Long accountId);

    /**
     * 批量插入账户角色关联
     * @param accountRolePOList 账户角色关联列表
     */
    void batchInsert(List<AccountRolePO> accountRolePOList);

    /**
     * 根据账户ID查询角色关联
     * @param accountId 账户ID
     * @return 账户角色关联列表
     */
    List<AccountRolePO> selectByAccountId(Long accountId);

    /**
     * 根据角色ID查询账户关联
     * @param roleId 角色ID
     * @return 账户角色关联列表
     */
    List<AccountRolePO> selectByRoleId(Long roleId);

    /**
     * 插入单条账户角色关联
     * @param accountRolePO 账户角色关联对象
     */
    void insert(AccountRolePO accountRolePO);
}
