package com.example.authservice.rpc.api;

import com.example.authservice.rpc.dto.request.AuthorizeRoleRpcRequest;

public interface AuthorizationRpcService {

    boolean authorizeRole(AuthorizeRoleRpcRequest request);
}
