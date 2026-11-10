package io.github.khaytul_illia.inventory_manager_api.support.context;

import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSessionRepository;
import io.github.khaytul_illia.inventory_manager_api.auth.token.RefreshTokenRepository;
import io.github.khaytul_illia.inventory_manager_api.support.config.TestcontainersConfig;
import io.github.khaytul_illia.inventory_manager_api.support.data.TestDataConstants;
import io.github.khaytul_illia.inventory_manager_api.support.data.UserTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.support.utils.e2e.AuthRequestUtils;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
public abstract class E2ETestContext {

    @Autowired
    protected RestTestClient restClient;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;
    @Autowired
    protected UserSessionRepository sessionRepository;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    @LocalServerPort
    protected int port;

    protected User operator;
    protected AuthRequestUtils authRequestUtils;

    @BeforeEach
    void createOperator(){
        authRequestUtils = new AuthRequestUtils(restClient, objectMapper);

        operator = userRepository.save(UserTestDataFactory.defaults()
            .password(passwordEncoder.encode(TestDataConstants.USER_PASSWORD))
            .role(User.UserRole.OPERATOR)
            .build()
        );
    }

    @AfterEach
    void baseCleanup(){
        refreshTokenRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }
    
}
