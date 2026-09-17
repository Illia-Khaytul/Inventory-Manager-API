package io.github.khaytul_illia.inventory_manager_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Application loading tests")
class InventoryManagerApiApplicationTests {

	@Autowired
	private PostgreSQLContainer postgres;

	@Test
	@DisplayName("Should load application context and PostgreSQL Testcontainer")
	void shouldLoadContextAndTestcontainers() {
		//Assert
		assertTrue(postgres.isRunning());
	}

}
