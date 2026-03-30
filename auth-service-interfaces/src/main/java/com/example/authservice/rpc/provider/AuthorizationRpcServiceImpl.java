package com.example.authservice.rpc.provider;

import com.example.authservice.authorization.usecase.AuthorizeRoleUseCase;
import com.example.authservice.authorization.usecase.command.AuthorizeRoleCommand;
import com.example.authservice.identity.usecase.AuthenticateUseCase;
import com.example.authservice.rpc.api.AuthorizationRpcService;
import com.example.authservice.rpc.dto.request.AuthorizeRoleRpcRequest;
import com.example.authservice.rpc.support.RpcTokenSupport;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class AuthorizationRpcServiceImpl implements AuthorizationRpcService {

    private final AuthorizeRoleUseCase authorizeRoleUseCase;
    private final AuthenticateUseCase authenticateUseCase;

    public AuthorizationRpcServiceImpl(AuthorizeRoleUseCase authorizeRoleUseCase,
                                       AuthenticateUseCase authenticateUseCase) {
        this.authorizeRoleUseCase = authorizeRoleUseCase;
        this.authenticateUseCase = authenticateUseCase;
    }

    @Override
    public boolean authorizeRole(AuthorizeRoleRpcRequest request) {
        authenticateUseCase.authenticate(RpcTokenSupport.resolveRawToken(request == null ? null : request.token()));
        authorizeRoleUseCase.batchAuthorize(new AuthorizeRoleCommand(
                request == null ? null : request.roleId(),
                request == null ? null : request.permissionIds()
        ));
        return true;
    }
}
