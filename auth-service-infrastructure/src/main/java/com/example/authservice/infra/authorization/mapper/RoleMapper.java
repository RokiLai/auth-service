package com.example.authservice.infra.authorization.mapper;

import com.example.authservice.infra.authorization.po.RolePO;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RoleMapper {
    /**
     * 查询所有角色
     * 
     * @return
     */
    List<RolePO> selectAll();

    /**
     * 根据角色ID列表查询角色
     * 
     * @param roleIds
     * @return
     */
    List<RolePO> selectByIds(List<Long> roleIds);

    /**
     * 新增角色
     * 
     * @param role
     * @return
     */
    int insert(RolePO role);

    /**
     * 根据角色ID查询角色
     * 
     * @param roleId
     * @return
     */
    RolePO selectById(Long roleId);
}
