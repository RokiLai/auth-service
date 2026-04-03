package com.example.authcenter.infra.redis;

import com.example.authcenter.infra.service.IdentitySessionSnapshot;
import com.example.authcenter.util.JsonUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigSerializationTest {

    @Test
    void jsonUtilShouldRoundTripIdentitySessionSnapshotRecord() {
        IdentitySessionSnapshot source = new IdentitySessionSnapshot("sid-123", 42L, "alice");

        String serialized = JsonUtil.toJson(source);
        IdentitySessionSnapshot restored = JsonUtil.toObj(serialized, IdentitySessionSnapshot.class);

        assertThat(restored).isEqualTo(source);
    }
}
