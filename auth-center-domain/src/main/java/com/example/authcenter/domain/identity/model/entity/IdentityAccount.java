package com.example.authcenter.domain.identity.model.entity;

import com.example.authcenter.domain.identity.model.valueobject.PasswordHash;
import com.example.authcenter.domain.identity.model.valueobject.RawPassword;
import com.example.authcenter.domain.identity.service.PasswordHasher;
import com.example.authcenter.exception.auth.PasswordTooShortException;
import io.micrometer.common.util.StringUtils;
import lombok.Getter;

@Getter
public class IdentityAccount {
    private final Long id;
    private final String username;
    private PasswordHash passwordHash;
    private final String email;

    IdentityAccount(Long id,
                    String username,
                    PasswordHash passwordHash,
                    String email) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
    }

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

}
