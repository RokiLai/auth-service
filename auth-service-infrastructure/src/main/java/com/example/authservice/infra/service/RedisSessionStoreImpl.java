package com.example.authservice.infra.service;

import com.example.authservice.auth.LoginSession;
import com.example.authservice.domain.service.SessionStore;
import com.example.authservice.infra.reids.RedisUtil;
import com.example.authservice.util.config.JwtProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisSessionStoreImpl implements SessionStore {

    private static final String SESSION_KEY_PREFIX = "login:session:";
    private static final String USER_SESSION_KEY_PREFIX = "login:user_session:";

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public void save(LoginSession session) {
        redisUtil.set(SESSION_KEY_PREFIX + session.getSessionId(), session, sessionTtl());
    }

    @Override
    public LoginSession getBySessionId(String sessionId) {
        return (LoginSession) redisUtil.get(SESSION_KEY_PREFIX + sessionId);
    }

    @Override
    public String getSessionIdByUserId(Long userId) {
        return (String) redisUtil.get(USER_SESSION_KEY_PREFIX + userId);
    }

    @Override
    public void bindUserSession(Long userId, String sessionId) {
        redisUtil.set(USER_SESSION_KEY_PREFIX + userId, sessionId, sessionTtl());
    }

    @Override
    public void deleteSession(String sessionId) {
        redisUtil.delete(SESSION_KEY_PREFIX + sessionId);
    }

    @Override
    public void deleteUserSession(Long userId) {
        redisUtil.delete(USER_SESSION_KEY_PREFIX + userId);
    }

    private Duration sessionTtl() {
        return Duration.ofMillis(jwtProperties.getExpire());
    }
}
