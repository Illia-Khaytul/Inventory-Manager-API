package io.github.khaytul_illia.inventory_manager_api.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/dummy")
public class DummyController {

    @PostMapping(path = "")
    public void dummyPost(
        @RequestBody @Validated DummyRequest request
    ){
        dummyOperation();
    }

    @PatchMapping(path = "/{id}")
    public void dummyPatch(
        @PathVariable @Valid @Positive @Min(2) long id,
        @RequestParam @Valid @NotEmpty String query
    ){
        dummyOperation();
    }

    @GetMapping(path = "")
    public void dummyPatch(){
        dummyOperation();
    }

    public void dummyOperation(){}

    public record DummyRequest(
        @NotNull
        String value1,
        @NotEmpty
        @Size(min = 2, max = 5)
        String value2
    ) {}

}
