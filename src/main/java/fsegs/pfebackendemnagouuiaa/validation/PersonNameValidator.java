package fsegs.pfebackendemnagouuiaa.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PersonNameValidator implements ConstraintValidator<PersonName, String> {

    private static final Pattern PERSON_NAME_PATTERN =
            Pattern.compile("^[\\p{L}\\s'\\-]+$", Pattern.UNICODE_CHARACTER_CLASS);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return PERSON_NAME_PATTERN.matcher(trimmed).matches();
    }
}
