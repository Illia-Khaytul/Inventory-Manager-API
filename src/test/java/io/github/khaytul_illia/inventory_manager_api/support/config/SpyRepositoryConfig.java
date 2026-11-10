package io.github.khaytul_illia.inventory_manager_api.support.config;

import io.github.khaytul_illia.inventory_manager_api.auth.token.RefreshTokenRepository;
import io.github.khaytul_illia.inventory_manager_api.product.ProductRepository;
import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockReset;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

@TestConfiguration
public class SpyRepositoryConfig {

    @Bean
    @Primary
    public RefreshTokenRepository refreshTokenRepositorySpy(RefreshTokenRepository refreshTokenRepository){
        return mock(RefreshTokenRepository.class,
            MockReset.withSettings(MockReset.AFTER)
                .defaultAnswer(delegatesTo(refreshTokenRepository)));
    }

    @Bean
    @Primary
    public ProductRepository productRepositorySpy(ProductRepository productRepository){
        return mock(ProductRepository.class,
            MockReset.withSettings(MockReset.AFTER)
                .defaultAnswer(delegatesTo(productRepository)));
    }

    @Bean
    @Primary
    public UserRepository userRepositorySpy(UserRepository userRepository){
        return mock(UserRepository.class,
            MockReset.withSettings(MockReset.AFTER)
                .defaultAnswer(delegatesTo(userRepository)));
    }

}
