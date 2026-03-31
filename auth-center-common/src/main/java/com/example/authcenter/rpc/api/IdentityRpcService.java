package com.example.authcenter.rpc.api;

import com.example.authcenter.rpc.dto.request.LoginRpcRequest;
import com.example.authcenter.rpc.dto.request.LogoutRpcRequest;
import com.example.authcenter.rpc.dto.request.RegisterRpcRequest;
import com.example.authcenter.rpc.dto.request.UpdatePasswordRpcRequest;
import com.example.authcenter.rpc.dto.response.LoginRpcResponse;

public interface IdentityRpcService {

    boolean register(RegisterRpcRequest request);

    LoginRpcResponse login(LoginRpcRequest request);

    boolean logout(LogoutRpcRequest request);

    boolean updatePassword(UpdatePasswordRpcRequest request);
}
