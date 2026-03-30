package com.example.authservice.domain.authorization.repository;


import java.util.List;

import com.example.authservice.domain.authorization.model.Role;

public interface RoleRepository {

    /**
     * 查询所有角色
     *
     * @return
     */
    List<Role> selectAll();

    /**
     * 根据角色ID列表查询角色
     *
     * @param roleIds
     * @return
     */
    List<Role> selectByIds(List<Long> roleIds);

    /**
     * 根据角色ID查询角色
     *
     * @param roleId
     * @return
     */
    Role selectById(Long roleId);

    /**
     * 根据id获取角色code
     */
    List<String> selectCodeByIds(List<Long> roleIds);


    /**
     * 保存账号角色关系
     *
     * @param role 角色
     */
    void updateRolePermission(Role role);
}
