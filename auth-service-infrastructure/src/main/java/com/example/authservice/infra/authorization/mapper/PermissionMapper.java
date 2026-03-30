package com.example.authservice.infra.authorization.mapper;

import com.example.authservice.infra.authorization.po.PermissionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PermissionMapper {

    PermissionPO selectById(@Param("permissionId") Long permissionId);

    List<PermissionPO> selectByIds(@Param("permissionIds") List<Long> permissionIds);

}
