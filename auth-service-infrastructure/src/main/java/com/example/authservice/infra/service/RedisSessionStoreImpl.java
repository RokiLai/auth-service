package com.example.authservice.infra.service;

import com.example.authservice.domain.identity.model.IdentitySession;
import com.example.authservice.domain.identity.repository.IdentitySessionRepository;
import com.example.authservice.infra.reids.RedisUtil;
import com.example.authservice.util.config.JwtProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisSessionStoreImpl implements IdentitySessionRepository {

    private static final String SESSION_KEY_PREFIX = "login:session:";
    private static final String USER_SESSION_KEY_PREFIX = "login:user_session:";

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public void save(IdentitySession session) {
        redisUtil.set(SESSION_KEY_PREFIX + session.getSessionId(), session, sessionTtl());
        redisUtil.set(USER_SESSION_KEY_PREFIX + session.getAccountId(), session.getSessionId(), sessionTtl());
    }

    @Override
    public IdentitySession findBySessionId(String sessionId) {
        return (IdentitySession) redisUtil.get(SESSION_KEY_PREFIX + sessionId);
    }

    @Override
    public String findSessionIdByAccountId(Long accountId) {
        return (String) redisUtil.get(USER_SESSION_KEY_PREFIX + accountId);
    }

    @Override
    public IdentitySession findByAccountId(Long accountId) {
        String sessionId = findSessionIdByAccountId(accountId);
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        return findBySessionId(sessionId);
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        redisUtil.delete(SESSION_KEY_PREFIX + sessionId);
    }

    @Override
    public void deleteByAccountId(Long accountId) {
        redisUtil.delete(USER_SESSION_KEY_PREFIX + accountId);
    }

    private Duration sessionTtl() {
        return Duration.ofMillis(jwtProperties.getExpire());
    }
}
