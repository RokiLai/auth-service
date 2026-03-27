package com.example.authservice.controller;

import com.example.authservice.auth.LoginSession;
import com.example.authservice.config.JwtInterceptor;
import com.example.authservice.config.WebConfig;
import com.example.authservice.domain.model.Account;
import com.example.authservice.domain.repo.AccountRepo;
import com.example.authservice.domain.repo.PermissionRepo;
import com.example.authservice.domain.repo.RolePermissionRepo;
import com.example.authservice.domain.repo.RoleRepo;
import com.example.authservice.domain.service.SessionStore;
import com.example.authservice.service.impl.AccountServiceImpl;
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
    private InMemorySessionStore sessionStore;

    @MockBean
    private AccountRepo accountRepo;

    @MockBean
    private RolePermissionRepo rolePermissionRepo;

    @MockBean
    private RoleRepo roleRepo;

    @MockBean
    private PermissionRepo permissionRepo;

    @BeforeEach
    void setUp() {
        sessionStore.clear();
    }

    @Test
    void loginLogoutFlowShouldCreateAndInvalidateSessionBackedToken() throws Exception {
        String username = "tester";
        String password = "123456";
        Account account = new Account(1L, username, password, "tester@example.com");
        when(accountRepo.findByUsername(username)).thenReturn(account);

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
        assertThat(sessionStore.getSessionIdByUserId(1L)).isEqualTo(sessionId);

        LoginSession loginSession = sessionStore.getBySessionId(sessionId);
        assertThat(loginSession).isNotNull();
        assertThat(loginSession.getAccountId()).isEqualTo(1L);
        assertThat(loginSession.getUsername()).isEqualTo(username);
        assertThat(loginSession.getToken()).isEqualTo(token);

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        assertThat(sessionStore.getBySessionId(sessionId)).isNull();
        assertThat(sessionStore.getSessionIdByUserId(1L)).isNull();

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40103))
                .andExpect(jsonPath("$.message").value("Token已过期，请重新登录"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            AccountController.class,
            AccountServiceImpl.class,
            JwtInterceptor.class,
            WebConfig.class,
            JwtUtil.class,
            JwtProperties.class
    })
    static class TestApplication {

        @Bean
        InMemorySessionStore sessionStore() {
            return new InMemorySessionStore();
        }
    }

    static class InMemorySessionStore implements SessionStore {

        private final Map<String, LoginSession> sessions = new ConcurrentHashMap<>();
        private final Map<Long, String> userSessions = new ConcurrentHashMap<>();

        @Override
        public void save(LoginSession session) {
            sessions.put(session.getSessionId(), session);
        }

        @Override
        public LoginSession getBySessionId(String sessionId) {
            return sessions.get(sessionId);
        }

        @Override
        public String getSessionIdByUserId(Long userId) {
            return userSessions.get(userId);
        }

        @Override
        public void bindUserSession(Long userId, String sessionId) {
            userSessions.put(userId, sessionId);
        }

        @Override
        public void deleteSession(String sessionId) {
            sessions.remove(sessionId);
        }

        @Override
        public void deleteUserSession(Long userId) {
            userSessions.remove(userId);
        }

        void clear() {
            sessions.clear();
            userSessions.clear();
        }
    }
}
