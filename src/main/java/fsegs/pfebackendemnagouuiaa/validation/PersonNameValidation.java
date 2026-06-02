package fsegs.pfebackendemnagouuiaa.validation;

import java.util.regex.Pattern;

/**
 * Validation metier partagee des noms / prenoms (hors annotations Jakarta).
 */
public final class PersonNameValidation {

    private static final Pattern PERSON_NAME_PATTERN =
            Pattern.compile("^[\\p{L}\\s'\\-]+$", Pattern.UNICODE_CHARACTER_CLASS);

    private PersonNameValidation() {
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty() && PERSON_NAME_PATTERN.matcher(trimmed).matches();
    }

    public static void requireValid(String value, String fieldLabel) {
        if (!isValid(value)) {
            throw new IllegalArgumentException(
                    fieldLabel + " : seules les lettres sont autorisees (lettres accentuees, espaces, tirets et apostrophes).");
        }
    }
}
