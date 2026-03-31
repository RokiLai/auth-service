package com.example.authcenter.grpc.support;

import com.example.authcenter.exception.auth.AuthBusinessException;
import com.example.authcenter.exception.auth.AuthenticationFailedException;
import com.example.authcenter.exception.auth.EmailInvalidException;
import com.example.authcenter.exception.auth.OldPasswordIncorrectException;
import com.example.authcenter.exception.auth.PasswordTooShortException;
import com.example.authcenter.exception.auth.TokenExpiredException;
import com.example.authcenter.exception.auth.TokenInvalidException;
import com.example.authcenter.exception.auth.TokenMissingException;
import com.example.authcenter.exception.auth.UsernameAlreadyExistsException;
import com.example.authcenter.exception.auth.UsernameRequiredException;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public final class GrpcServiceSupport {

    private static final Logger log = LoggerFactory.getLogger(GrpcServiceSupport.class);

    private GrpcServiceSupport() {
    }

    public static <T> void unary(StreamObserver<T> observer, Supplier<T> supplier) {
        try {
            observer.onNext(supplier.get());
            observer.onCompleted();
        } catch (Throwable ex) {
            observer.onError(toStatus(ex));
        }
    }

    private static RuntimeException toStatus(Throwable ex) {
        if (ex instanceof StatusRuntimeException runtimeException) {
            return runtimeException;
        }
        if (ex instanceof StatusException statusException) {
            return statusException.getStatus().withDescription(statusException.getMessage()).asRuntimeException();
        }

        Status status = mapStatus(ex);
        if (status.getCode() == Status.Code.INTERNAL) {
            log.error("gRPC service failed", ex);
        }
        return status.withDescription(ex.getMessage()).asRuntimeException();
    }

    private static Status mapStatus(Throwable ex) {
        if (ex instanceof UsernameAlreadyExistsException
                || ex instanceof UsernameRequiredException
                || ex instanceof PasswordTooShortException
                || ex instanceof EmailInvalidException
                || ex instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT;
        }
        if (ex instanceof TokenMissingException
                || ex instanceof TokenExpiredException
                || ex instanceof TokenInvalidException
                || ex instanceof AuthenticationFailedException) {
            return Status.UNAUTHENTICATED;
        }
        if (ex instanceof OldPasswordIncorrectException) {
            return Status.FAILED_PRECONDITION;
        }
        if (ex instanceof AuthBusinessException) {
            return Status.FAILED_PRECONDITION;
        }
        return Status.INTERNAL;
    }
}
