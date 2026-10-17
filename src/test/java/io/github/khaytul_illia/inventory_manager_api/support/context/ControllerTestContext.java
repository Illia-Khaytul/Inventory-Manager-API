package io.github.khaytul_illia.inventory_manager_api.support.context;

import io.github.khaytul_illia.inventory_manager_api.auth.AuthService;
import io.github.khaytul_illia.inventory_manager_api.common.validation.SizePropertyValidator;
import io.github.khaytul_illia.inventory_manager_api.common.validation.ValidationProperties;
import io.github.khaytul_illia.inventory_manager_api.product.ProductService;
import io.github.khaytul_illia.inventory_manager_api.security.AuthenticationErrorHandler;
import io.github.khaytul_illia.inventory_manager_api.security.AuthorizationErrorHandler;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityConfig;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtAuthenticationErrorHandler;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtConfig;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtRsaPemKeyConfig;
import io.github.khaytul_illia.inventory_manager_api.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest
@Import({
    SecurityConfig.class,
    JwtRsaPemKeyConfig.class,
    JwtConfig.class,
    JwtAuthenticationErrorHandler.class,
    AuthenticationErrorHandler.class,
    AuthorizationErrorHandler.class,
    SizePropertyValidator.class,
    ValidationProperties.class
})
@TestPropertySource(properties = "logging.level.io.github.khaytul_illia.inventory_manager_api=DEBUG")
@ActiveProfiles("test")
public abstract class ControllerTestContext {

    @MockitoBean
    protected AuthService authService;
    @MockitoBean
    protected ProductService productService;
    @MockitoBean
    protected UserService userService;
    @MockitoBean
    protected SecurityUtils securityUtils;

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

}
