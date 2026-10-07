package io.github.khaytul_illia.inventory_manager_api.common.resilience;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.resilience.annotation.EnableResilientMethods;

@Configuration
@EnableResilientMethods(order = Ordered.HIGHEST_PRECEDENCE)
public class RetryableConfig {
}
