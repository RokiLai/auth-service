package com.example.authservice.infra.authorization.mapper;

import com.example.authservice.infra.authorization.po.RolePermissionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RolePermissionMapper {

    /**
     * 查询某角色的所有权限关联记录
     */
    List<RolePermissionPO> findByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除角色所有权限关联
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色权限关系
     */
    void insertRolePermissions(@Param("roleId") Long roleId,
                               @Param("permissionIds") List<Long> permissionIds);

    /**
     * 根据多个角色 ID 查询关联的权限关系
     */
    List<RolePermissionPO> findByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 根据角色 ID 查询关联的权限关系
     * @param permissionId
     * @return
     */
    List<RolePermissionPO> findByPermissionId(@Param("permissionId") Long permissionId);

}
