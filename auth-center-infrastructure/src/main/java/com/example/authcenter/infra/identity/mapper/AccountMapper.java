package com.example.authcenter.infra.identity.mapper;

import com.example.authcenter.infra.identity.po.AccountPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {
    /**
     * 根据用户名查询 AccountPO
     * @param username 用户名
     * @return AccountPO 对象，如果不存在则返回 null
     */
    AccountPO findByUsername(@Param("username") String username);

    /**
     * 插入新的 AccountPO
     * @param accountPO 待插入的 AccountPO
     * @return 影响的行数
     */
    int insert(AccountPO accountPO);

    /**
     * 更新 AccountPO
     * @param accountPO 待更新的 AccountPO
     * @return 影响的行数
     */
    int update(AccountPO accountPO);
}
