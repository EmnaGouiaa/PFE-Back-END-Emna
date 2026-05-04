package fsegs.pfebackendemnagouuiaa.services;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CredentialPolicyService {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+?";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$"
    );

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateStrongPassword() {
        List<Character> characters = new ArrayList<>();
        characters.add(randomChar(UPPER));
        characters.add(randomChar(LOWER));
        characters.add(randomChar(DIGITS));
        characters.add(randomChar(SPECIAL));

        while (characters.size() < 12) {
            characters.add(randomChar(ALL));
        }

        Collections.shuffle(characters, secureRandom);
        StringBuilder builder = new StringBuilder();
        characters.forEach(builder::append);
        return builder.toString();
    }

    public void validatePasswordStrength(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins 8 caracteres, une majuscule, une minuscule, un chiffre et un caractere special."
            );
        }
    }

    private char randomChar(String alphabet) {
        return alphabet.charAt(secureRandom.nextInt(alphabet.length()));
    }
}
