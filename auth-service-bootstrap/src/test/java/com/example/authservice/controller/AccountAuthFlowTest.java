package com.example.authservice.controller;

import com.example.authservice.config.JwtInterceptor;
import com.example.authservice.config.WebConfig;
import com.example.authservice.domain.identity.model.IdentityAccount;
import com.example.authservice.domain.identity.model.IdentitySession;
import com.example.authservice.domain.identity.repository.IdentityAccountRepository;
import com.example.authservice.domain.identity.repository.IdentitySessionRepository;
import com.example.authservice.domain.repo.PermissionRepo;
import com.example.authservice.domain.repo.RolePermissionRepo;
import com.example.authservice.domain.repo.RoleRepo;
import com.example.authservice.identity.usecase.impl.AuthenticateUseCaseImpl;
import com.example.authservice.identity.usecase.impl.LoginUseCaseImpl;
import com.example.authservice.identity.usecase.impl.LogoutUseCaseImpl;
import com.example.authservice.infra.identity.service.BcryptPasswordHasher;
import com.example.authservice.infra.identity.service.JwtIdentityTokenProvider;
import com.example.authservice.service.AccountService;
import com.example.authservice.util.JwtUtil;
import com.example.authservice.util.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AccountAuthFlowTest.TestApplication.class,
        properties = {
                "jwt.secret=your-256-bit-secret-your-256-bit-secret",
                "jwt.expire=3600000"
        }
)
@AutoConfigureMockMvc
class AccountAuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private InMemoryIdentitySessionRepository sessionRepository;

    @Autowired
    private BcryptPasswordHasher passwordHasher;

    @MockBean
    private IdentityAccountRepository identityAccountRepository;

    @MockBean
    private RolePermissionRepo rolePermissionRepo;

    @MockBean
    private RoleRepo roleRepo;

    @MockBean
    private PermissionRepo permissionRepo;

    @MockBean
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        sessionRepository.clear();
    }

    @Test
    void loginLogoutFlowShouldCreateAndInvalidateSessionBackedToken() throws Exception {
        String username = "tester";
        String password = "123456";
        IdentityAccount account = new IdentityAccount(
                1L,
                username,
                passwordHasher.encode(new com.example.authservice.domain.identity.model.RawPassword(password)),
                "tester@example.com",
                null
        );
        when(identityAccountRepository.findByUsername(username)).thenReturn(account);

        String token = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"tester","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(header().exists("Authorization"))
                .andReturn()
                .getResponse()
                .getHeader("Authorization");

        assertThat(token).isNotBlank();

        String sessionId = jwtUtil.parseSessionId(token);
        assertThat(sessionId).isNotBlank();
        assertThat(jwtUtil.parseUserId(token)).isEqualTo(1L);
        assertThat(sessionRepository.findByAccountId(1L)).isNotNull();
        assertThat(sessionRepository.findByAccountId(1L).getSessionId()).isEqualTo(sessionId);

        IdentitySession loginSession = sessionRepository.findBySessionId(sessionId);
        assertThat(loginSession).isNotNull();
        assertThat(loginSession.getAccountId()).isEqualTo(1L);
        assertThat(loginSession.getUsername()).isEqualTo(username);
        assertThat(loginSession.getToken()).isEqualTo(token);

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        assertThat(sessionRepository.findBySessionId(sessionId)).isNull();
        assertThat(sessionRepository.findByAccountId(1L)).isNull();

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40103))
                .andExpect(jsonPath("$.message").value("Token已过期，请重新登录"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            IdentityController.class,
            JwtInterceptor.class,
            WebConfig.class,
            JwtUtil.class,
            JwtProperties.class,
            JwtIdentityTokenProvider.class,
            BcryptPasswordHasher.class,
            LoginUseCaseImpl.class,
            LogoutUseCaseImpl.class,
            AuthenticateUseCaseImpl.class
    })
    static class TestApplication {

        @Bean
        InMemoryIdentitySessionRepository identitySessionRepository() {
            return new InMemoryIdentitySessionRepository();
        }
    }

    static class InMemoryIdentitySessionRepository implements IdentitySessionRepository {

        private final Map<String, IdentitySession> sessions = new ConcurrentHashMap<>();
        private final Map<Long, String> userSessions = new ConcurrentHashMap<>();

        @Override
        public void save(IdentitySession session) {
            sessions.put(session.getSessionId(), session);
            userSessions.put(session.getAccountId(), session.getSessionId());
        }

        @Override
        public IdentitySession findBySessionId(String sessionId) {
            return sessions.get(sessionId);
        }

        @Override
        public IdentitySession findByAccountId(Long accountId) {
            String sessionId = userSessions.get(accountId);
            if (sessionId == null) {
                return null;
            }
            return sessions.get(sessionId);
        }

        @Override
        public void deleteBySessionId(String sessionId) {
            sessions.remove(sessionId);
        }

        @Override
        public void deleteByAccountId(Long accountId) {
            userSessions.remove(accountId);
        }

        void clear() {
            sessions.clear();
            userSessions.clear();
        }
    }
}
