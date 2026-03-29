package com.example.authservice.domain.identity.model.entity;

import com.example.authservice.domain.identity.model.valueobject.PasswordHash;
import com.example.authservice.domain.identity.model.valueobject.RawPassword;
import com.example.authservice.domain.identity.service.PasswordHasher;
import com.example.authservice.exception.auth.EmailInvalidException;
import com.example.authservice.exception.auth.PasswordTooShortException;
import com.example.authservice.exception.auth.UsernameRequiredException;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IdentityAccountFactory {

    /**
     * 注册时由工厂统一校验并创建身份账号，实体本身只承载行为与状态。
     * The factory centralizes registration-time validation and construction so the entity focuses on state and behavior.
     */
    public IdentityAccount register(String username,
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

        return new IdentityAccount(
                null,
                username,
                passwordHasher.encode(rawPassword),
                email,
                List.of()
        );
    }

    /**
     * 仓储从持久化层重建账号实体时统一走重建工厂，避免实体暴露静态恢复入口。
     * Reconstitutes the entity from persistence through an explicit factory instead of static restore methods on the entity.
     */
    public IdentityAccount restore(Long id,
                                   String username,
                                   PasswordHash passwordHash,
                                   String email,
                                   List<Long> roleIds) {
        return new IdentityAccount(id, username, passwordHash, email, roleIds);
    }
}
