package com.example.authservice.domain.authorization.repository;

import java.util.List;

public interface PermissionRepository {

    List<String>  selectCodeByIds(List<Long> permissionIds);
}
