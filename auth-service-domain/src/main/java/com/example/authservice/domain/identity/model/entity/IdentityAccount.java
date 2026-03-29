package com.example.authservice.domain.identity.model.entity;

import com.example.authservice.domain.identity.model.valueobject.PasswordHash;
import com.example.authservice.domain.identity.model.valueobject.RawPassword;
import com.example.authservice.domain.identity.service.PasswordHasher;
import com.example.authservice.exception.auth.EmailInvalidException;
import com.example.authservice.exception.auth.PasswordTooShortException;
import com.example.authservice.exception.auth.UsernameRequiredException;
import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IdentityAccount {
    private Long id;
    private String username;
    private PasswordHash passwordHash;
    private String email;
    private List<Long> roleIds;

    /**
     * 认证阶段只关心原始密码是否能匹配当前账号的密码摘要。
     */
    public boolean matchPassword(RawPassword rawPassword, PasswordHasher passwordHasher) {
        return rawPassword != null && passwordHasher.matches(rawPassword, passwordHash);
    }

    /**
     * 修改密码时仍由身份领域实体维护密码规则，避免应用层直接操作密码摘要。
     * The identity aggregate keeps password-update rules so the application layer does not mutate password hashes directly.
     */
    public void updatePassword(RawPassword newPassword, PasswordHasher passwordHasher) {
        if (newPassword == null || StringUtils.isBlank(newPassword.value()) || newPassword.value().length() < 6) {
            throw new PasswordTooShortException();
        }
        this.passwordHash = passwordHasher.encode(newPassword);
    }

    /**
     * 注册时直接创建身份账号实体，避免认证上下文继续依赖旧账号模型。
     * Registration now creates the identity account directly so the identity context no longer depends on the legacy account model.
     */
    public static IdentityAccount register(String username,
                                           RawPassword rawPassword,
                                           String email,
                                           PasswordHasher passwordHasher) {
        if (StringUtils.isBlank(username)) {
            throw new UsernameRequiredException();
        }
        if (rawPassword == null || StringUtils.isBlank(rawPassword.value()) || rawPassword.value().length() < 6) {
            throw new PasswordTooShortException();
        }
        if (StringUtils.isBlank(email) || !email.contains("@")) {
            throw new EmailInvalidException();
        }

        IdentityAccount account = new IdentityAccount();
        account.username = username;
        account.passwordHash = passwordHasher.encode(rawPassword);
        account.email = email;
        return account;
    }
}
