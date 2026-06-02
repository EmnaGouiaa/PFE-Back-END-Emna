package fsegs.pfebackendemnagouuiaa.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PersonNameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PersonName {

    String message() default "Seules les lettres sont autorisees (lettres accentuees, espaces, tirets et apostrophes).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
