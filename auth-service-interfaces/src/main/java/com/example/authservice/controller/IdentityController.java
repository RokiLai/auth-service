package com.example.authservice.controller;

import com.example.authservice.application.context.CurrentOperator;
import com.example.authservice.annotation.AuthIdentity;
import com.example.authservice.annotation.PassToken;
import com.example.authservice.controller.request.LoginRequest;
import com.example.authservice.domain.identity.model.result.CurrentIdentity;
import com.example.authservice.identity.dto.LoginResult;
import com.example.authservice.identity.usecase.LoginUseCase;
import com.example.authservice.identity.usecase.LogoutUseCase;
import com.example.authservice.identity.usecase.command.LogoutCommand;
import com.example.authservice.service.dto.UserLoginDTO;
import com.roki.exception.result.Result;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class IdentityController {

    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private LoginUseCase loginUseCase;

    @Autowired
    private LogoutUseCase logoutUseCase;

    @PassToken
    @PostMapping("/login")
    public Result<UserLoginDTO> login(@Valid @RequestBody LoginRequest request,
                                      HttpServletResponse response) {
        LoginResult loginResult = loginUseCase.login(request.getUsername(), request.getPassword());
        UserLoginDTO userLoginDTO = new UserLoginDTO(
                loginResult.getId(),
                loginResult.getUsername(),
                loginResult.getEmail(),
                loginResult.getToken()
        );
        response.setHeader("Authorization", BEARER_PREFIX + userLoginDTO.getToken());
        response.setHeader("Access-Control-Expose-Headers", "Authorization");
        return Result.success(userLoginDTO);
    }

    @PostMapping("/logout")
    // 当前登录身份由接口层注入，再显式传入应用用例。
    // The current authenticated identity is injected at the interface layer and then passed explicitly to the use case.
    public Result<Boolean> logout(@AuthIdentity CurrentIdentity currentIdentity) {
        // 控制器只负责把接口层身份对象转换成应用层命令对象。
        // The controller only translates the interface-layer identity into an application command object.
        return Result.success(logoutUseCase.logout(new LogoutCommand(CurrentOperator.from(currentIdentity))));
    }
}
