package com.example.authservice.rpc.api;

import com.example.authservice.rpc.dto.request.LoginRpcRequest;
import com.example.authservice.rpc.dto.request.LogoutRpcRequest;
import com.example.authservice.rpc.dto.request.RegisterRpcRequest;
import com.example.authservice.rpc.dto.request.UpdatePasswordRpcRequest;
import com.example.authservice.rpc.dto.response.LoginRpcResponse;

public interface IdentityRpcService {

    boolean register(RegisterRpcRequest request);

    LoginRpcResponse login(LoginRpcRequest request);

    boolean logout(LogoutRpcRequest request);

    boolean updatePassword(UpdatePasswordRpcRequest request);
}
