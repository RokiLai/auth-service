package com.example.authservice.infra.authorization.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountRolePO {
    private Long accountId;
    private Long roleId;
}

