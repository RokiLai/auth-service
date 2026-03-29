package com.example.authservice.controller;

import com.example.authservice.config.JwtInterceptor;
import com.example.authservice.config.WebConfig;
import com.example.authservice.config.CurrentIdentityArgumentResolver;
import com.example.authservice.domain.identity.model.entity.IdentityAccount;
import com.example.authservice.domain.identity.model.entity.IdentitySession;
import com.example.authservice.domain.identity.model.result.AuthorizationSnapshot;
import com.example.authservice.domain.identity.model.result.CurrentIdentity;
import com.example.authservice.application.context.CurrentOperator;
import com.example.authservice.domain.identity.repository.IdentityAccountRepository;
import com.example.authservice.domain.identity.repository.IdentitySessionRepository;
import com.example.authservice.domain.identity.service.impl.AuthenticationDomainServiceImpl;
import com.example.authservice.domain.identity.service.AuthorizationSnapshotProvider;
import com.example.authservice.identity.usecase.AuthenticateUseCase;
import com.example.authservice.identity.usecase.LogoutUseCase;
import com.example.authservice.identity.usecase.command.LogoutCommand;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AccountAuthFlowTest.TestApplication.class,
        properties = {
                "jwt.secret=your-256-bit-secret-your-256-bit-secret",
                "jwt.expire=3600000",
                "roki.exception.error-code.project-code=10",
                "roki.exception.error-code.default-biz-code=01",
                "roki.exception.error-code.biz-codes.auth=01"
        }
)
@AutoConfigureMockMvc
class AccountAuthFlowTest {

    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private InMemoryIdentitySessionRepository sessionRepository;

    @Autowired
    private BcryptPasswordHasher passwordHasher;

    @Autowired
    private AuthenticateUseCase authenticateUseCase;

    @Autowired
    private LogoutUseCase logoutUseCase;

    @MockBean
    private IdentityAccountRepository identityAccountRepository;

    @MockBean
    private AuthorizationSnapshotProvider authorizationSnapshotProvider;

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
                passwordHasher.encode(new com.example.authservice.domain.identity.model.valueobject.RawPassword(password)),
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
                .andExpect(header().string("Authorization", org.hamcrest.Matchers.startsWith(BEARER_PREFIX)))
                .andReturn()
                .getResponse()
                .getHeader("Authorization");

        token = bearerToken(token);

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
                        .header("Authorization", BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        assertThat(sessionRepository.findBySessionId(sessionId)).isNull();
        assertThat(sessionRepository.findByAccountId(1L)).isNull();

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001103))
                .andExpect(jsonPath("$.message").value("Token已过期，请重新登录"));
    }

    @Test
    void secondLoginShouldInvalidatePreviousToken() throws Exception {
        String username = "tester";
        String password = "123456";
        IdentityAccount account = new IdentityAccount(
                1L,
                username,
                passwordHasher.encode(new com.example.authservice.domain.identity.model.valueobject.RawPassword(password)),
                "tester@example.com",
                null
        );
        when(identityAccountRepository.findByUsername(username)).thenReturn(account);

        String firstToken = bearerToken(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"tester","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Authorization"));

        String secondToken = bearerToken(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"tester","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Authorization"));

        assertThat(firstToken).isNotEqualTo(secondToken);

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", BEARER_PREFIX + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001103))
                .andExpect(jsonPath("$.message").value("Token已过期，请重新登录"));

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", BEARER_PREFIX + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void invalidLoginRequestShouldBeRejectedBeforeBusinessLogic() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"username":"","password":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(9001101))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is("用户名不能为空"),
                        org.hamcrest.Matchers.is("密码不能为空")
                )));

        verifyNoInteractions(identityAccountRepository);
    }

    @Test
    void missingTokenShouldBeRejectedByInterceptor() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001102))
                .andExpect(jsonPath("$.message").value("缺少Token，请先登录"));
    }

    @Test
    void emptyBearerTokenShouldBeRejectedAsInvalid() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", BEARER_PREFIX))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001104))
                .andExpect(jsonPath("$.message").value("Token无效"));
    }

    @Test
    void bareTokenShouldBeRejectedWhenBearerPrefixIsMissing() throws Exception {
        String username = "tester";
        String password = "123456";
        IdentityAccount account = new IdentityAccount(
                1L,
                username,
                passwordHasher.encode(new com.example.authservice.domain.identity.model.valueobject.RawPassword(password)),
                "tester@example.com",
                null
        );
        when(identityAccountRepository.findByUsername(username)).thenReturn(account);

        String token = bearerToken(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"tester","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Authorization"));

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001104))
                .andExpect(jsonPath("$.message").value("Token无效"));
    }

    @Test
    void tamperedBearerTokenShouldBeRejectedAsInvalid() throws Exception {
        String username = "tester";
        String password = "123456";
        IdentityAccount account = new IdentityAccount(
                1L,
                username,
                passwordHasher.encode(new com.example.authservice.domain.identity.model.valueobject.RawPassword(password)),
                "tester@example.com",
                null
        );
        when(identityAccountRepository.findByUsername(username)).thenReturn(account);

        String token = bearerToken(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"tester","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Authorization"));

        String tamperedToken = token + "tampered";

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", BEARER_PREFIX + tamperedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001104))
                .andExpect(jsonPath("$.message").value("Token无效"));
    }


    @Test
    void staleAuthenticatedRequestShouldNotDeleteNewSessionOnLogout() throws Exception {
        String username = "tester";
        String password = "123456";
        IdentityAccount account = new IdentityAccount(
                1L,
                username,
                passwordHasher.encode(new com.example.authservice.domain.identity.model.valueobject.RawPassword(password)),
                "tester@example.com",
                null
        );
        when(identityAccountRepository.findByUsername(username)).thenReturn(account);

        String oldToken = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"tester","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Authorization");

        oldToken = bearerToken(oldToken);

        CurrentIdentity staleIdentity = authenticateUseCase.authenticate(oldToken);

        String newToken = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"tester","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Authorization");

        newToken = bearerToken(newToken);

        String oldSessionId = jwtUtil.parseSessionId(oldToken);
        String newSessionId = jwtUtil.parseSessionId(newToken);

        assertThat(oldSessionId).isNotEqualTo(newSessionId);
        assertThat(sessionRepository.findBySessionId(oldSessionId)).isNull();
        assertThat(sessionRepository.findBySessionId(newSessionId)).isNotNull();

        assertThat(logoutUseCase.logout(new LogoutCommand(CurrentOperator.from(staleIdentity)))).isTrue();

        assertThat(sessionRepository.findBySessionId(newSessionId)).isNotNull();
        assertThat(sessionRepository.findSessionIdByAccountId(1L)).isEqualTo(newSessionId);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            IdentityController.class,
            JwtInterceptor.class,
            CurrentIdentityArgumentResolver.class,
            WebConfig.class,
            JwtUtil.class,
            JwtProperties.class,
            JwtIdentityTokenProvider.class,
            BcryptPasswordHasher.class,
            AuthenticationDomainServiceImpl.class,
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
        public String findSessionIdByAccountId(Long accountId) {
            return userSessions.get(accountId);
        }

        @Override
        public IdentitySession findByAccountId(Long accountId) {
            String sessionId = findSessionIdByAccountId(accountId);
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

    private static String bearerToken(String authorizationHeader) {
        assertThat(authorizationHeader).startsWith(BEARER_PREFIX);
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
