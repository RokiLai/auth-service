package com.example.authcenter.infra.service;

import com.example.authcenter.domain.identity.model.entity.IdentitySession;
import com.example.authcenter.domain.identity.model.entity.IdentitySessionFactory;
import com.example.authcenter.infra.redis.RedisUtil;
import com.example.authcenter.util.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisSessionStoreImplTest {

    private final IdentitySessionFactory identitySessionFactory = new IdentitySessionFactory();

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RedisSessionStoreImpl sessionStore;

    @Test
    void saveAndBindUserSessionShouldUseExpectedRedisKeysAndJwtTtl() {
        when(jwtProperties.getExpire()).thenReturn(3_600_000L);

        IdentitySession session = identitySessionFactory.restore("sid-123", 42L, null, null);

        sessionStore.save(session);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(redisUtil).set(org.mockito.ArgumentMatchers.eq("login:session:sid-123"), org.mockito.ArgumentMatchers.same(session), ttlCaptor.capture());
        verify(redisUtil).set("login:user_session:42", "sid-123", Duration.ofMillis(3_600_000L));
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMillis(3_600_000L));
    }

    @Test
    void deleteOperationsShouldTargetExpectedRedisKeys() {
        sessionStore.deleteBySessionId("sid-123");
        sessionStore.deleteByAccountId(42L);

        verify(redisUtil).delete("login:session:sid-123");
        verify(redisUtil).delete("login:user_session:42");
    }

    @Test
    void findByAccountIdShouldResolveSessionIdFromUserBinding() {
        IdentitySession session = identitySessionFactory.restore("sid-123", null, null, null);
        when(redisUtil.get("login:user_session:42")).thenReturn("sid-123");
        when(redisUtil.get("login:session:sid-123")).thenReturn(session);

        assertThat(sessionStore.findSessionIdByAccountId(42L)).isEqualTo("sid-123");
        assertThat(sessionStore.findByAccountId(42L)).isSameAs(session);
    }

    @Test
    void findByAccountIdShouldReturnNullWhenNoUserBindingExists() {
        when(redisUtil.get("login:user_session:42")).thenReturn(null);

        assertThat(sessionStore.findSessionIdByAccountId(42L)).isNull();
        assertThat(sessionStore.findByAccountId(42L)).isNull();
        verify(redisUtil, never()).get("login:session:null");
    }
}
