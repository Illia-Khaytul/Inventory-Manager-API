package io.github.khaytul_illia.inventory_manager_api.error.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class InvalidStockModificationException extends RuntimeException {

    private List<String> details;

    public InvalidStockModificationException(String message, String... details) {
        super(message);

        this.details = List.of(details);
    }

}
