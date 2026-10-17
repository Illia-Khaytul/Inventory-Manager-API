package io.github.khaytul_illia.inventory_manager_api.support.context;

import io.github.khaytul_illia.inventory_manager_api.auth.token.TokenFactory;
import io.github.khaytul_illia.inventory_manager_api.support.config.SpyRepositoryConfig;
import io.github.khaytul_illia.inventory_manager_api.support.config.TestcontainersConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({
    TestcontainersConfig.class,
    SpyRepositoryConfig.class
})
@ActiveProfiles("test")
public abstract class ServiceITContext {

    @MockitoSpyBean
    protected TokenFactory tokenFactory;

    @Autowired
    protected TransactionTemplate transactionTemplate;
    
}
