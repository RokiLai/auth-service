package com.example.authcenter.infra.service;

import com.example.authcenter.domain.identity.model.entity.IdentitySession;
import com.example.authcenter.domain.identity.model.entity.IdentitySessionFactory;
import com.example.authcenter.util.JsonUtil;
import com.example.authcenter.util.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisSessionStoreImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtProperties jwtProperties;

    private final IdentitySessionFactory identitySessionFactory = new IdentitySessionFactory();
    private RedisSessionStoreImpl sessionStore;

    @BeforeEach
    void setUp() {
        sessionStore = new RedisSessionStoreImpl(redisTemplate, jwtProperties, identitySessionFactory);
    }

    @Test
    void saveAndBindUserSessionShouldUseExpectedRedisKeysAndJwtTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtProperties.getExpire()).thenReturn(3_600_000L);

        IdentitySession session = identitySessionFactory.restore("sid-123", 42L, null);

        sessionStore.save(session);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        ArgumentCaptor<String> cacheCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq("login:session:sid-123"), cacheCaptor.capture(), ttlCaptor.capture());
        verify(valueOperations).set("login:user_session:42", "sid-123", Duration.ofMillis(3_600_000L));
        IdentitySessionSnapshot sessionSnapshot = JsonUtil.toObj(cacheCaptor.getValue(), IdentitySessionSnapshot.class);
        assertThat(sessionSnapshot.sessionId()).isEqualTo("sid-123");
        assertThat(sessionSnapshot.accountId()).isEqualTo(42L);
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMillis(3_600_000L));
    }

    @Test
    void deleteOperationsShouldTargetExpectedRedisKeys() {
        sessionStore.deleteBySessionId("sid-123");
        sessionStore.deleteByAccountId(42L);

        verify(redisTemplate).delete("login:session:sid-123");
        verify(redisTemplate).delete("login:user_session:42");
    }

    @Test
    void findByAccountIdShouldResolveSessionIdFromUserBinding() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:user_session:42")).thenReturn("sid-123");
        when(valueOperations.get("login:session:sid-123"))
                .thenReturn(JsonUtil.toJson(new IdentitySessionSnapshot("sid-123", 42L, "alice")));

        assertThat(sessionStore.findSessionIdByAccountId(42L)).isEqualTo("sid-123");
        IdentitySession session = sessionStore.findByAccountId(42L);
        assertThat(session).isNotNull();
        assertThat(session.getSessionId()).isEqualTo("sid-123");
        assertThat(session.getAccountId()).isEqualTo(42L);
        assertThat(session.getUsername()).isEqualTo("alice");
    }

    @Test
    void findByAccountIdShouldReturnNullWhenNoUserBindingExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:user_session:42")).thenReturn(null);

        assertThat(sessionStore.findSessionIdByAccountId(42L)).isNull();
        assertThat(sessionStore.findByAccountId(42L)).isNull();
        verify(valueOperations, never()).get("login:session:null");
    }
}
