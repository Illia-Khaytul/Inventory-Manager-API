package io.github.khaytul_illia.inventory_manager_api.user.password;

import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidPasswordException;
import org.passay.DefaultPasswordValidator;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.ValidationResult;
import org.passay.data.EnglishCharacterData;
import org.passay.resolver.PropertiesMessageResolver;
import org.passay.rule.CharacterRule;
import org.passay.rule.LengthRule;
import org.passay.rule.WhitespaceRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserPasswordValidator {

    private final PasswordValidator passwordValidator;

    public UserPasswordValidator(
        @Value("${app.user.password.min-length}")
        int minLength,
        @Value("${app.user.password.max-length}")
        int maxLength,
        @Value("${app.user.password.min-digit-amount}")
        int minDigitAmount
    ){
        passwordValidator = new DefaultPasswordValidator(new PropertiesMessageResolver(), List.of(
            new LengthRule(minLength, maxLength),
            new CharacterRule(EnglishCharacterData.Digit, minDigitAmount),
            new WhitespaceRule()
        ));
    }

    public void validateUserPassword(String password){
        if(password == null || password.isBlank()){
            throw new InvalidPasswordException("Invalid password", List.of("User password cannot be null or blank."));
        }

        ValidationResult validationResult = passwordValidator.validate(new PasswordData(password));
        if(!validationResult.isValid()){
            System.out.println(validationResult.getMessages());
            throw new InvalidPasswordException("Invalid password", validationResult.getMessages());
        }
    }

}
