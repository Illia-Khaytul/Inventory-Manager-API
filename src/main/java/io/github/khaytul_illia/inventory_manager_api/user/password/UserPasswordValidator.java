package io.github.khaytul_illia.inventory_manager_api.user.password;

import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidPasswordException;
import org.passay.DefaultPasswordValidator;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.ValidationResult;
import org.passay.data.EnglishCharacterData;
import org.passay.rule.CharacterRule;
import org.passay.rule.LengthRule;
import org.passay.rule.WhitespaceRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserPasswordValidator {

    private final PasswordValidator passwordValidator;

    public UserPasswordValidator(){
        passwordValidator = new DefaultPasswordValidator(List.of(
            new LengthRule(6, 50),
            new CharacterRule(EnglishCharacterData.Digit, 2),
            new WhitespaceRule()
        ));
    }

    public void validateUserPassword(String password){
        ValidationResult validationResult = passwordValidator.validate(new PasswordData(password));
        if(!validationResult.isValid()){
            throw new InvalidPasswordException("Invalid password", validationResult.getMessages());
        }
    }

}
