package com.example.authservice.domain.service;

import com.example.authservice.auth.LoginSession;

public interface SessionStore {

    void save(LoginSession session);

    LoginSession getBySessionId(String sessionId);

    String getSessionIdByUserId(Long userId);

    void bindUserSession(Long userId, String sessionId);

    void deleteSession(String sessionId);

    void deleteUserSession(Long userId);
}
