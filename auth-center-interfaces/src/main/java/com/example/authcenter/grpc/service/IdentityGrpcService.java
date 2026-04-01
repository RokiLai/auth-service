package com.example.authcenter.grpc.service;

import com.example.authcenter.grpc.IdentityServiceGrpc;
import com.example.authcenter.grpc.TokenRequest;
import com.example.authcenter.grpc.UserInfoReply;
import com.example.authcenter.grpc.support.GrpcServiceSupport;
import com.example.authcenter.identity.usecase.ValidateTokenUseCase;
import com.example.authcenter.identity.usecase.result.ValidatedUserResult;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class IdentityGrpcService extends IdentityServiceGrpc.IdentityServiceImplBase {

    private final ValidateTokenUseCase validateTokenUseCase;

    public IdentityGrpcService(ValidateTokenUseCase validateTokenUseCase) {
        this.validateTokenUseCase = validateTokenUseCase;
    }

    @Override
    public void validateToken(TokenRequest request, StreamObserver<UserInfoReply> responseObserver) {
        GrpcServiceSupport.unary(responseObserver, () -> {
            ValidatedUserResult result = validateTokenUseCase.validate(resolveRawToken(request.getToken()));
            return UserInfoReply.newBuilder()
                    .setId(result.id())
                    .setUsername(result.username())
                    .setEmail(result.email())
                    .build();
        });
    }

    private String resolveRawToken(String token) {
        if (token == null) {
            return null;
        }
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }
}
