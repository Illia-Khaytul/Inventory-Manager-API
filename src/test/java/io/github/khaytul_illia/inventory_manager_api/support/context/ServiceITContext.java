package io.github.khaytul_illia.inventory_manager_api.support.context;

import io.github.khaytul_illia.inventory_manager_api.auth.token.TokenFactory;
import io.github.khaytul_illia.inventory_manager_api.support.config.TestcontainersConfiguration;
import io.github.khaytul_illia.inventory_manager_api.product.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class ServiceITContext {

    @MockitoSpyBean
    protected TokenFactory tokenFactory;
    @MockitoSpyBean
    protected ProductMapper productMapper;
    @MockitoSpyBean
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected TransactionTemplate transactionTemplate;
    
}
