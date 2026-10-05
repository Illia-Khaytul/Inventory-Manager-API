package io.github.khaytul_illia.inventory_manager_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class InventoryManagerApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryManagerApiApplication.class, args);
	}

}
