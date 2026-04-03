package com.example.authcenter.controller;

import com.example.authcenter.application.context.CurrentOperator;
import com.example.authcenter.annotation.AuthIdentity;
import com.example.authcenter.annotation.PassToken;
import com.example.authcenter.controller.request.LoginRequest;
import com.example.authcenter.controller.request.RegisterRequest;
import com.example.authcenter.controller.request.UpdatePasswordRequest;
import com.example.authcenter.controller.response.LoginResponse;
import com.example.authcenter.identity.usecase.LoginUseCase;
import com.example.authcenter.identity.usecase.LogoutUseCase;
import com.example.authcenter.identity.usecase.RegisterUseCase;
import com.example.authcenter.identity.usecase.UpdatePasswordUseCase;
import com.example.authcenter.identity.usecase.command.LogoutCommand;
import com.example.authcenter.identity.usecase.command.RegisterCommand;
import com.example.authcenter.identity.usecase.command.UpdatePasswordCommand;
import com.example.authcenter.identity.usecase.result.LoginResult;
import com.roki.exception.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Identity")
@RequestMapping("/auth")
public class IdentityController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RegisterUseCase registerUseCase;
    private final UpdatePasswordUseCase updatePasswordUseCase;

    public IdentityController(LoginUseCase loginUseCase,
                              LogoutUseCase logoutUseCase,
                              RegisterUseCase registerUseCase,
                              UpdatePasswordUseCase updatePasswordUseCase) {
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.registerUseCase = registerUseCase;
        this.updatePasswordUseCase = updatePasswordUseCase;
    }

    @PassToken
    @PostMapping("/register")
    @Operation(summary = "注册账号")
    public Result<Boolean> register(@Valid @RequestBody RegisterRequest request) {
        registerUseCase.register(new RegisterCommand(
                request.username(),
                request.password(),
                request.email()
        ));
        return Result.success(true);
    }

    @PassToken
    @PostMapping("/login")
    @Operation(summary = "账号登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletResponse response) {
        LoginResult loginResult = loginUseCase.login(request.username(), request.password());
        LoginResponse loginResponse = new LoginResponse(
                loginResult.id(),
                loginResult.username(),
                loginResult.email(),
                loginResult.token()
        );
        response.setHeader("Authorization", BEARER_PREFIX + loginResponse.token());
        response.setHeader("Access-Control-Expose-Headers", "Authorization");
        return Result.success(loginResponse);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "退出登录",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    // 当前登录身份由接口层注入，再显式传入应用用例。
    // The current authenticated identity is injected at the interface layer and then passed explicitly to the use case.
    public Result<Boolean> logout(
            @Parameter(hidden = true) @AuthIdentity CurrentOperator currentOperator) {
        return Result.success(logoutUseCase.logout(new LogoutCommand(currentOperator)));
    }

    @PostMapping("/update-password")
    @Operation(
            summary = "修改密码",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    // 改密属于身份能力，由 identity 控制器承接认证上下文并转给应用用例。
    // Password updates belong to the identity capability, so the identity controller maps authenticated context into the use case command.
    public Result<Boolean> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request,
            @Parameter(hidden = true) @AuthIdentity CurrentOperator currentOperator) {
        updatePasswordUseCase.updatePassword(new UpdatePasswordCommand(
                currentOperator,
                request.oldPassword(),
                request.newPassword()
        ));
        return Result.success(true);
    }
}
