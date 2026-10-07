package io.github.khaytul_illia.inventory_manager_api.common.auditing;

import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(modifyOnCreate = false)
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> authAuditorProvider(SecurityUtils securityUtils){
        return () -> Optional.of(securityUtils.getAuthenticatedUserAccessToken().getSubject());
    }

}
