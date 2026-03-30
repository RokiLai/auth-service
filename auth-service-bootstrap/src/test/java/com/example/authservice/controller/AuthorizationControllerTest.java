package com.example.authservice.controller;

import com.example.authservice.authorization.usecase.AuthorizeRoleUseCase;
import com.example.authservice.authorization.usecase.command.AuthorizeRoleCommand;
import com.example.authservice.config.CurrentOperatorArgumentResolver;
import com.example.authservice.config.JwtInterceptor;
import com.example.authservice.config.WebConfig;
import com.example.authservice.domain.authorization.service.AuthorizationDomainService;
import com.example.authservice.domain.identity.model.entity.IdentityAccount;
import com.example.authservice.domain.identity.model.entity.IdentityAccountFactory;
import com.example.authservice.domain.identity.model.entity.IdentitySession;
import com.example.authservice.domain.identity.model.entity.IdentitySessionFactory;
import com.example.authservice.domain.identity.repository.IdentityAccountRepository;
import com.example.authservice.domain.identity.repository.IdentitySessionRepository;
import com.example.authservice.domain.identity.service.impl.AuthenticationDomainServiceImpl;
import com.example.authservice.identity.usecase.AuthenticateUseCase;
import com.example.authservice.identity.usecase.LogoutUseCase;
import com.example.authservice.identity.usecase.LoginUseCase;
import com.example.authservice.identity.usecase.RegisterUseCase;
import com.example.authservice.identity.usecase.UpdatePasswordUseCase;
import com.example.authservice.identity.usecase.impl.AuthenticateUseCaseImpl;
import com.example.authservice.identity.usecase.impl.LoginUseCaseImpl;
import com.example.authservice.identity.usecase.impl.LogoutUseCaseImpl;
import com.example.authservice.identity.usecase.impl.UpdatePasswordUseCaseImpl;
import com.example.authservice.infra.identity.service.BcryptPasswordHasher;
import com.example.authservice.infra.identity.service.JwtIdentityTokenProvider;
import com.example.authservice.util.JwtUtil;
import com.example.authservice.util.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AuthorizationControllerTest.TestApplication.class,
        properties = {
                "jwt.secret=your-256-bit-secret-your-256-bit-secret",
                "jwt.expire=3600000",
                "roki.exception.error-code.project-code=10",
                "roki.exception.error-code.default-biz-code=01",
                "roki.exception.error-code.biz-codes.auth=01"
        }
)
@AutoConfigureMockMvc
class AuthorizationControllerTest {

    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryIdentitySessionRepository sessionRepository;

    @Autowired
    private BcryptPasswordHasher passwordHasher;

    @Autowired
    private IdentityAccountFactory identityAccountFactory;

    @MockBean
    private IdentityAccountRepository identityAccountRepository;

    @MockBean
    private AuthorizationDomainService authorizationDomainService;

    @MockBean
    private AuthorizeRoleUseCase authorizeRoleUseCase;

    @MockBean
    private RegisterUseCase registerUseCase;

    @MockBean
    private UpdatePasswordUseCase updatePasswordUseCase;

    @BeforeEach
    void setUp() {
        sessionRepository.clear();
    }

    @Test
    void authorizeRoleShouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/authorization/roles/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleId":1,"permissionIds":[2,3]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001102))
                .andExpect(jsonPath("$.message").value("缺少Token，请先登录"));

        verifyNoInteractions(authorizeRoleUseCase);
    }

    @Test
    void authorizeRoleShouldDelegateCommandWhenAuthenticated() throws Exception {
        String token = login("tester", "123456");

        mockMvc.perform(post("/authorization/roles/authorize")
                        .header("Authorization", BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleId":1,"permissionIds":[2,3]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        ArgumentCaptor<AuthorizeRoleCommand> commandCaptor = ArgumentCaptor.forClass(AuthorizeRoleCommand.class);
        verify(authorizeRoleUseCase).batchAuthorize(commandCaptor.capture());
        assertThat(commandCaptor.getValue().roleId()).isEqualTo(1L);
        assertThat(commandCaptor.getValue().permissionIds()).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    void authorizeRoleShouldRejectEmptyPermissionIdsBeforeUseCase() throws Exception {
        String token = login("tester", "123456");

        mockMvc.perform(post("/authorization/roles/authorize")
                        .header("Authorization", BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleId":1,"permissionIds":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(9001101))
                .andExpect(jsonPath("$.message").value("权限ID列表不能为空"));

        verifyNoInteractions(authorizeRoleUseCase);
    }

    private String login(String username, String password) throws Exception {
        IdentityAccount account = identityAccountFactory.restore(
                1L,
                username,
                passwordHasher.encode(new com.example.authservice.domain.identity.model.valueobject.RawPassword(password)),
                "tester@example.com",
                null
        );
        when(identityAccountRepository.findByUsername(username)).thenReturn(account);

        String authorizationHeader = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"tester","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Authorization");
        assertThat(authorizationHeader).startsWith(BEARER_PREFIX);
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            IdentityController.class,
            AuthorizationController.class,
            JwtInterceptor.class,
            CurrentOperatorArgumentResolver.class,
            WebConfig.class,
            JwtUtil.class,
            JwtProperties.class,
            JwtIdentityTokenProvider.class,
            BcryptPasswordHasher.class,
            IdentityAccountFactory.class,
            IdentitySessionFactory.class,
            AuthenticationDomainServiceImpl.class,
            LoginUseCaseImpl.class,
            LogoutUseCaseImpl.class,
            AuthenticateUseCaseImpl.class,
            UpdatePasswordUseCaseImpl.class
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
}
