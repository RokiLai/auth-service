package com.example.authservice.controller;

import com.example.authservice.application.context.CurrentOperator;
import com.example.authservice.annotation.AuthIdentity;
import com.example.authservice.annotation.PassToken;
import com.example.authservice.controller.request.RegisterRequest;
import com.example.authservice.controller.request.UpdatePasswordRequest;
import com.example.authservice.domain.identity.model.result.CurrentIdentity;
import com.example.authservice.service.AccountService;
import com.example.authservice.service.command.UpdatePasswordCommand;
import com.roki.exception.result.Result;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AccountController {
    @Autowired
    AccountService accountService;

    @PassToken
    @PostMapping("/register")
    public Result<Boolean> register(@Valid @RequestBody RegisterRequest request) {
        accountService.register(request.getUsername(), request.getPassword(), request.getEmail(), null);
        return Result.success(true);
    }

    @PostMapping("/update-password")
    // 控制器负责把认证上下文转换成应用层入参，应用层不再自行读取请求上下文。
    // The controller converts authentication context into use-case inputs so the application layer stays transport-agnostic.
    public Result<Boolean> updatePassword(@Valid @RequestBody UpdatePasswordRequest request,
                                          @AuthIdentity CurrentIdentity currentIdentity) {
        accountService.updatePassword(new UpdatePasswordCommand(
                CurrentOperator.from(currentIdentity),
                request.getOldPassword(),
                request.getNewPassword()
        ));
        return Result.success(true);
    }
}
