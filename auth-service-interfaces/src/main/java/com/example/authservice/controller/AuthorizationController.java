package com.example.authservice.controller;

import com.example.authservice.authorization.usecase.AuthorizeRoleUseCase;
import com.example.authservice.authorization.usecase.command.AuthorizeRoleCommand;
import com.example.authservice.controller.request.AuthorizeRoleRequest;
import com.roki.exception.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authorization")
public class AuthorizationController {

    private final AuthorizeRoleUseCase authorizeRoleUseCase;

    public AuthorizationController(AuthorizeRoleUseCase authorizeRoleUseCase) {
        this.authorizeRoleUseCase = authorizeRoleUseCase;
    }

    @PostMapping("/roles/authorize")
    // 授权接口只负责把请求映射为命令并交给应用层，不在控制器中承接规则判断。
    // The controller only maps the request into a command and delegates to the application layer.
    public Result<Boolean> authorizeRole(@Valid @RequestBody AuthorizeRoleRequest request) {
        authorizeRoleUseCase.batchAuthorize(new AuthorizeRoleCommand(
                request.roleId(),
                request.permissionIds()
        ));
        return Result.success(true);
    }
}
