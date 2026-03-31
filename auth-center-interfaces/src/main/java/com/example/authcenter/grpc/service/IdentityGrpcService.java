package com.example.authcenter.grpc.service;

import com.example.authcenter.application.context.CurrentOperator;
import com.example.authcenter.grpc.IdentityServiceGrpc;
import com.example.authcenter.grpc.LoginReply;
import com.example.authcenter.grpc.LoginRequest;
import com.example.authcenter.grpc.OperationReply;
import com.example.authcenter.grpc.RegisterRequest;
import com.example.authcenter.grpc.TokenRequest;
import com.example.authcenter.grpc.UpdatePasswordRequest;
import com.example.authcenter.grpc.support.GrpcServiceSupport;
import com.example.authcenter.identity.usecase.AuthenticateUseCase;
import com.example.authcenter.identity.usecase.LoginUseCase;
import com.example.authcenter.identity.usecase.LogoutUseCase;
import com.example.authcenter.identity.usecase.RegisterUseCase;
import com.example.authcenter.identity.usecase.UpdatePasswordUseCase;
import com.example.authcenter.identity.usecase.command.LogoutCommand;
import com.example.authcenter.identity.usecase.command.RegisterCommand;
import com.example.authcenter.identity.usecase.command.UpdatePasswordCommand;
import com.example.authcenter.identity.usecase.result.LoginResult;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class IdentityGrpcService extends IdentityServiceGrpc.IdentityServiceImplBase {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RegisterUseCase registerUseCase;
    private final UpdatePasswordUseCase updatePasswordUseCase;
    private final AuthenticateUseCase authenticateUseCase;

    public IdentityGrpcService(LoginUseCase loginUseCase,
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
    public void register(RegisterRequest request, StreamObserver<OperationReply> responseObserver) {
        GrpcServiceSupport.unary(responseObserver, () -> {
            boolean success = registerUseCase.register(new RegisterCommand(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail()
            ));
            return OperationReply.newBuilder().setSuccess(success).build();
        });
    }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginReply> responseObserver) {
        GrpcServiceSupport.unary(responseObserver, () -> {
            LoginResult result = loginUseCase.login(request.getUsername(), request.getPassword());
            return LoginReply.newBuilder()
                    .setId(result.id())
                    .setUsername(result.username())
                    .setEmail(result.email())
                    .setToken(result.token())
                    .build();
        });
    }

    @Override
    public void logout(TokenRequest request, StreamObserver<OperationReply> responseObserver) {
        GrpcServiceSupport.unary(responseObserver, () -> {
            CurrentOperator currentOperator = authenticateUseCase.authenticate(resolveRawToken(request.getToken()));
            boolean success = logoutUseCase.logout(new LogoutCommand(currentOperator));
            return OperationReply.newBuilder().setSuccess(success).build();
        });
    }

    @Override
    public void updatePassword(UpdatePasswordRequest request, StreamObserver<OperationReply> responseObserver) {
        GrpcServiceSupport.unary(responseObserver, () -> {
            CurrentOperator currentOperator = authenticateUseCase.authenticate(resolveRawToken(request.getToken()));
            boolean success = updatePasswordUseCase.updatePassword(new UpdatePasswordCommand(
                    currentOperator,
                    request.getOldPassword(),
                    request.getNewPassword()
            ));
            return OperationReply.newBuilder().setSuccess(success).build();
        });
    }

    private String resolveRawToken(String token) {
        if (token == null) {
            return null;
        }
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }
}
