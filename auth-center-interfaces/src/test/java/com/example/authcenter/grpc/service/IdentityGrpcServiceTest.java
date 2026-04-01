package com.example.authcenter.grpc.service;

import com.example.authcenter.grpc.TokenRequest;
import com.example.authcenter.grpc.UserInfoReply;
import com.example.authcenter.identity.usecase.ValidateTokenUseCase;
import com.example.authcenter.identity.usecase.result.ValidatedUserResult;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityGrpcServiceTest {

    private final ValidateTokenUseCase validateTokenUseCase = mock(ValidateTokenUseCase.class);
    private final IdentityGrpcService identityGrpcService = new IdentityGrpcService(validateTokenUseCase);

    @Test
    void validateTokenShouldReturnUserInfo() {
        when(validateTokenUseCase.validate("token-value"))
                .thenReturn(new ValidatedUserResult(1L, "tester", "tester@example.com"));

        RecordingObserver<UserInfoReply> observer = new RecordingObserver<>();

        identityGrpcService.validateToken(
                TokenRequest.newBuilder().setToken("Bearer token-value").build(),
                observer
        );

        assertThat(observer.reply).isNotNull();
        assertThat(observer.reply.getId()).isEqualTo(1L);
        assertThat(observer.reply.getUsername()).isEqualTo("tester");
        assertThat(observer.reply.getEmail()).isEqualTo("tester@example.com");
        assertThat(observer.completed).isTrue();
    }

    private static final class RecordingObserver<T> implements StreamObserver<T> {
        private T reply;
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value) {
            this.reply = value;
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }
    }
}
