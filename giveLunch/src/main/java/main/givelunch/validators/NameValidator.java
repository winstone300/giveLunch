package main.givelunch.validators;

import org.springframework.stereotype.Component;

@Component
public class NameValidator {
    public boolean isValid(final String name) {
        return name != null && !name.isBlank();
    }
}
