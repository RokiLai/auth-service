package com.example.authservice.infra.converter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.example.authservice.domain.authorization.model.Role;
import org.springframework.util.CollectionUtils;

import com.example.authservice.infra.po.RolePO;

public class RoleConverter {
    public static RolePO toPO(Role role) {
        if (role == null) {
            return null;
        }
        RolePO rolePO = new RolePO();
        rolePO.setId(role.getId());
        return rolePO;
    }

    public static Role toEntity(RolePO rolePO) {
        if (rolePO == null) {
            return null;
        }
        return Role.restore(rolePO.getId(), rolePO.getCode(), List.of());
    }

    public static List<Role> convertList(List<RolePO> rolePOs) {
        if (CollectionUtils.isEmpty(rolePOs)) {
            return Collections.emptyList();
        }
        return rolePOs.stream().map(RoleConverter::toEntity).collect(Collectors.toList());
    }




}
