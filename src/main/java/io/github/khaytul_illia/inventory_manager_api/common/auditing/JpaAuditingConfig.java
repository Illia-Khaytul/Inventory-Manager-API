package io.github.khaytul_illia.inventory_manager_api.common.auditing;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(modifyOnCreate = false)
public class JpaAuditingConfig {
}
