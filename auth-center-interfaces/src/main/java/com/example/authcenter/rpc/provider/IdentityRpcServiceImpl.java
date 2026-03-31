package com.example.authcenter.rpc.provider;

import com.example.authcenter.application.context.CurrentOperator;
import com.example.authcenter.identity.usecase.AuthenticateUseCase;
import com.example.authcenter.identity.usecase.LoginUseCase;
import com.example.authcenter.identity.usecase.LogoutUseCase;
import com.example.authcenter.identity.usecase.RegisterUseCase;
import com.example.authcenter.identity.usecase.UpdatePasswordUseCase;
import com.example.authcenter.identity.usecase.command.LogoutCommand;
import com.example.authcenter.identity.usecase.command.RegisterCommand;
import com.example.authcenter.identity.usecase.command.UpdatePasswordCommand;
import com.example.authcenter.identity.usecase.result.LoginResult;
import com.example.authcenter.rpc.api.IdentityRpcService;
import com.example.authcenter.rpc.dto.request.LoginRpcRequest;
import com.example.authcenter.rpc.dto.request.LogoutRpcRequest;
import com.example.authcenter.rpc.dto.request.RegisterRpcRequest;
import com.example.authcenter.rpc.dto.request.UpdatePasswordRpcRequest;
import com.example.authcenter.rpc.dto.response.LoginRpcResponse;
import com.example.authcenter.rpc.support.RpcTokenSupport;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class IdentityRpcServiceImpl implements IdentityRpcService {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RegisterUseCase registerUseCase;
    private final UpdatePasswordUseCase updatePasswordUseCase;
    private final AuthenticateUseCase authenticateUseCase;

    public IdentityRpcServiceImpl(LoginUseCase loginUseCase,
                                  LogoutUseCase logoutUseCase,
                                  RegisterUseCase registerUseCase,
                                  UpdatePasswordUseCase updatePasswordUseCase,
                                  AuthenticateUseCase authenticateUseCase) {
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.registerUseCase = registerUseCase;
        this.updatePasswordUseCase = updatePasswordUseCase;
        this.authenticateUseCase = authenticateUseCase;
    }

    @Override
    public boolean register(RegisterRpcRequest request) {
        return registerUseCase.register(new RegisterCommand(
                request == null ? null : request.username(),
                request == null ? null : request.password(),
                request == null ? null : request.email()
        ));
    }

    @Override
    public LoginRpcResponse login(LoginRpcRequest request) {
        LoginResult result = loginUseCase.login(
                request == null ? null : request.username(),
                request == null ? null : request.password()
        );
        return new LoginRpcResponse(result.id(), result.username(), result.email(), result.token());
    }

    @Override
    public boolean logout(LogoutRpcRequest request) {
        CurrentOperator currentOperator = authenticateUseCase.authenticate(
                RpcTokenSupport.resolveRawToken(request == null ? null : request.token())
        );
        return logoutUseCase.logout(new LogoutCommand(currentOperator));
    }

    @Override
    public boolean updatePassword(UpdatePasswordRpcRequest request) {
        CurrentOperator currentOperator = authenticateUseCase.authenticate(
                RpcTokenSupport.resolveRawToken(request == null ? null : request.token())
        );
        return updatePasswordUseCase.updatePassword(new UpdatePasswordCommand(
                currentOperator,
                request == null ? null : request.oldPassword(),
                request == null ? null : request.newPassword()
        ));
    }
}
