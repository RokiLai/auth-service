package com.example.authservice.authorization.usecase;

import com.example.authservice.authorization.usecase.command.AuthorizeRoleCommand;

public interface AuthorizeRoleUseCase {
    /**
     * 批量授权
     * Batch-authorizes a role with the latest permission set.
     */
    void batchAuthorize(AuthorizeRoleCommand command);
}
