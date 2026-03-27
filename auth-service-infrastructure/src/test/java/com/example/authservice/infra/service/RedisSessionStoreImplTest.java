package com.example.authservice.infra.service;

import com.example.authservice.auth.LoginSession;
import com.example.authservice.infra.reids.RedisUtil;
import com.example.authservice.util.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisSessionStoreImplTest {

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RedisSessionStoreImpl sessionStore;

    @Test
    void saveAndBindUserSessionShouldUseExpectedRedisKeysAndJwtTtl() {
        when(jwtProperties.getExpire()).thenReturn(3_600_000L);

        LoginSession session = new LoginSession();
        session.setSessionId("sid-123");
        session.setAccountId(42L);

        sessionStore.save(session);
        sessionStore.bindUserSession(42L, "sid-123");

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(redisUtil).set(org.mockito.ArgumentMatchers.eq("login:session:sid-123"), org.mockito.ArgumentMatchers.same(session), ttlCaptor.capture());
        verify(redisUtil).set("login:user_session:42", "sid-123", Duration.ofMillis(3_600_000L));
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMillis(3_600_000L));
    }

    @Test
    void deleteOperationsShouldTargetExpectedRedisKeys() {
        sessionStore.deleteSession("sid-123");
        sessionStore.deleteUserSession(42L);

        verify(redisUtil).delete("login:session:sid-123");
        verify(redisUtil).delete("login:user_session:42");
    }
}
