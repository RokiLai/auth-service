package com.example.authservice.service.impl;

import com.example.authservice.auth.AccountContextHolder;
import com.example.authservice.auth.AccountInfo;
import com.example.authservice.auth.LoginSession;
import com.example.authservice.domain.repo.*;
import com.example.authservice.domain.service.SessionStore;
import com.example.authservice.exception.AuthErrorCode;
import com.example.authservice.service.AccountService;
import com.example.authservice.service.dto.UserLoginDTO;
import com.example.authservice.domain.model.Account;
import com.example.authservice.util.JwtUtil;
import com.roki.exception.BusinessException;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SessionStore sessionStore;

    @Autowired
    private RolePermissionRepo rolePermissionRepo;

    @Autowired
    private RoleRepo roleRepo;

    private final PermissionRepo permissionRepo;

    public AccountServiceImpl(AccountRepo accountRepo, JwtUtil jwtUtil, SessionStore sessionStore,
                              RolePermissionRepo rolePermissionRepo, RoleRepo roleRepo, PermissionRepo permissionRepo) {
        this.accountRepo = accountRepo;
        this.jwtUtil = jwtUtil;
        this.sessionStore = sessionStore;
        this.rolePermissionRepo = rolePermissionRepo;
        this.roleRepo = roleRepo;
        this.permissionRepo = permissionRepo;
    }

    @Override
    public boolean register(String username, String password, String email, List<Long> roleIds) {
        Account account = accountRepo.findByUsername(username);
        if (account != null) {
            throw new BusinessException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }
        account = Account.register(username, password, email);
        accountRepo.save(account);

        if (!CollectionUtils.isEmpty(roleIds)) {
            account.setRoleIds(roleIds);
            accountRepo.updateAccountRole(account);
        }
        return true;
    }

    @Override
    public UserLoginDTO login(String username, String password) {
        Account account = accountRepo.findByUsername(username);
        if (account == null || !account.matchPassword(password)) {
            throw new BusinessException(AuthErrorCode.LOGIN_FAILED);
        }

        String oldSessionId = sessionStore.getSessionIdByUserId(account.getId());
        if (oldSessionId != null && !oldSessionId.isBlank()) {
            sessionStore.deleteSession(oldSessionId);
            sessionStore.deleteUserSession(account.getId());
        }

        String sessionId = UUID.randomUUID().toString();
        String token = jwtUtil.generateToken(account.getId(), username, sessionId);
        UserLoginDTO result = new UserLoginDTO(account.getId(), account.getUsername(), account.getEmail(), token);

        LoginSession loginSession = new LoginSession();
        loginSession.setSessionId(sessionId);
        loginSession.setAccountId(account.getId());
        loginSession.setUsername(account.getUsername());
        loginSession.setToken(token);

        if (!CollectionUtils.isEmpty(account.getRoleIds())) {
            List<String> roles = roleRepo.selectCodeByIds(account.getRoleIds());
            loginSession.setRoles(roles);

            List<Long> permissionIds = rolePermissionRepo.findPermissionIdsByRoleIds(account.getRoleIds());
            List<String> permissions = permissionRepo.selectCodeByIds(permissionIds);
            loginSession.setPermissions(permissions);
        }

        sessionStore.save(loginSession);
        sessionStore.bindUserSession(account.getId(), sessionId);
        return result;
    }

    @Override
    public boolean validatePassword(String username, String password) {
        Account account = accountRepo.findByUsername(username);
        return account != null && account.matchPassword(password);
    }

    @Override
    public boolean updatePassword(String oldPassword, String newPassword) {
        AccountInfo currentAccount = AccountContextHolder.get();
        String username = currentAccount.getUsername();
        Account account = accountRepo.findByUsername(username);
        if (account == null || !account.matchPassword(oldPassword)) {
            throw new BusinessException(AuthErrorCode.OLD_PASSWORD_INCORRECT);
        }
        account.updatePassword(newPassword);
        accountRepo.save(account);
        if (currentAccount.getId() != null) {
            String sessionId = sessionStore.getSessionIdByUserId(currentAccount.getId());
            if (sessionId != null && !sessionId.isBlank()) {
                sessionStore.deleteSession(sessionId);
            }
            sessionStore.deleteUserSession(currentAccount.getId());
        }
        return true;
    }

    @Override
    public boolean logout() {
        AccountInfo currentAccount = AccountContextHolder.get();
        if (currentAccount == null || currentAccount.getId() == null) {
            throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
        }

        String sessionId = sessionStore.getSessionIdByUserId(currentAccount.getId());
        if (sessionId != null && !sessionId.isBlank()) {
            sessionStore.deleteSession(sessionId);
        }
        sessionStore.deleteUserSession(currentAccount.getId());
        return true;
    }

}
