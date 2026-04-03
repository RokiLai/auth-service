package com.example.authcenter.infra.service;

import com.example.authcenter.domain.identity.model.entity.IdentitySession;
import com.example.authcenter.domain.identity.model.entity.IdentitySessionFactory;
import com.example.authcenter.domain.identity.repository.IdentitySessionRepository;
import com.example.authcenter.util.JsonUtil;
import com.example.authcenter.util.config.JwtProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisSessionStoreImpl implements IdentitySessionRepository {

    private static final String SESSION_KEY_PREFIX = "login:session:";
    private static final String USER_SESSION_KEY_PREFIX = "login:user_session:";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;
    private final IdentitySessionFactory identitySessionFactory;

    public RedisSessionStoreImpl(RedisTemplate<String, String> redisTemplate,
                                 JwtProperties jwtProperties,
                                 IdentitySessionFactory identitySessionFactory) {
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
        this.identitySessionFactory = identitySessionFactory;
    }

    @Override
    public void save(IdentitySession session) {
        redisTemplate.opsForValue().set(
                SESSION_KEY_PREFIX + session.getSessionId(),
                JsonUtil.toJson(IdentitySessionSnapshot.from(session)),
                sessionTtl()
        );
        redisTemplate.opsForValue().set(
                USER_SESSION_KEY_PREFIX + session.getAccountId(),
                session.getSessionId(),
                sessionTtl()
        );
    }

    @Override
    public IdentitySession findBySessionId(String sessionId) {
        String sessionCacheJson = redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + sessionId);
        if (sessionCacheJson == null || sessionCacheJson.isBlank()) {
            return null;
        }
        IdentitySessionSnapshot sessionSnapshot = JsonUtil.toObj(sessionCacheJson, IdentitySessionSnapshot.class);
        return identitySessionFactory.restore(
                sessionSnapshot.sessionId(),
                sessionSnapshot.accountId(),
                sessionSnapshot.username()
        );
    }

    @Override
    public String findSessionIdByAccountId(Long accountId) {
        return redisTemplate.opsForValue().get(USER_SESSION_KEY_PREFIX + accountId);
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
        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
    }

    @Override
    public void deleteByAccountId(Long accountId) {
        redisTemplate.delete(USER_SESSION_KEY_PREFIX + accountId);
    }

    private Duration sessionTtl() {
        return Duration.ofMillis(jwtProperties.getExpire());
    }
}
