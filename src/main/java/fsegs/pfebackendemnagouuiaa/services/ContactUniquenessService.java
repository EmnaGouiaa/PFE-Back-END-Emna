package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.exception.DuplicateFieldException;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ContactUniquenessService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9][0-9 .()\\-]{7,19}$");

    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;

    public String normalizeAndValidateRequiredEmail(String email, String fieldName) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire.");
        }

        String normalized = email.trim().toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException(resolveEmailMessage(fieldName));
        }
        return normalized;
    }

    public String normalizeAndValidateOptionalEmail(String email, String fieldName) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String normalized = email.trim().toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException(resolveEmailMessage(fieldName));
        }
        return normalized;
    }

    public String normalizeAndValidateOptionalPhone(String telephone, String fieldName) {
        if (telephone == null || telephone.trim().isEmpty()) {
            return null;
        }

        String normalized = telephone.trim();
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(resolvePhoneMessage(fieldName));
        }
        return normalized;
    }

    public String normalizeAndValidateRequiredPhone(String telephone, String fieldName) {
        if (telephone == null || telephone.trim().isEmpty()) {
            throw new IllegalArgumentException(resolveRequiredPhoneMessage(fieldName));
        }

        String normalized = telephone.trim();
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(resolvePhoneMessage(fieldName));
        }
        return normalized;
    }

    public String normalizeAndValidateOptionalMatricule(String matricule, String fieldName) {
        if (matricule == null || matricule.trim().isEmpty()) {
            return null;
        }

        return matricule.trim();
    }

    public void validateUserContactForCreate(String email, String telephone) {
        validateUserIdentityForCreate(email, telephone, null);
    }

    public void validateUserContactForUpdate(Long userId, String email, String telephone) {
        validateUserIdentityForUpdate(userId, email, telephone, null);
    }

    public void validateUserIdentityForCreate(String email, String telephone, String matricule) {
        validateEmailAvailable(email, "email", null, null);
        validateTelephoneAvailable(telephone, "telephone", null, null);
        validateMatriculeAvailable(matricule, null);
    }

    public void validateUserIdentityForUpdate(Long userId, String email, String telephone, String matricule) {
        validateEmailAvailable(email, "email", userId, null);
        validateTelephoneAvailable(telephone, "telephone", userId, null);
        validateMatriculeAvailable(matricule, userId);
    }

    public void validateEntrepriseContactForCreate(String email, String telephone) {
        validateEmailAvailable(email, "emailEntreprise", null, null);
        validateTelephoneAvailable(telephone, "telephoneEntreprise", null, null);
    }

    public void validateEntrepriseContactForUpdate(Long entrepriseId, String email, String telephone) {
        validateEmailAvailable(email, "emailEntreprise", null, entrepriseId);
        validateTelephoneAvailable(telephone, "telephoneEntreprise", null, entrepriseId);
    }

    private void validateEmailAvailable(String email, String fieldName, Long currentUserId, Long currentEntrepriseId) {
        if (email == null || email.isBlank()) {
            return;
        }

        utilisateurRepository.findByEmailIgnoreCase(email)
                .filter(existing -> currentUserId == null || !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new DuplicateFieldException(fieldName, resolveDuplicateEmailMessage(fieldName));
                });

        entrepriseRepository.findByEmailIgnoreCase(email)
                .filter(existing -> currentEntrepriseId == null || !existing.getId().equals(currentEntrepriseId))
                .ifPresent(existing -> {
                    throw new DuplicateFieldException(fieldName, resolveDuplicateEmailMessage(fieldName));
                });
    }

    private void validateTelephoneAvailable(String telephone, String fieldName, Long currentUserId, Long currentEntrepriseId) {
        if (telephone == null || telephone.isBlank()) {
            return;
        }

        utilisateurRepository.findByTelephone(telephone)
                .filter(existing -> currentUserId == null || !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new DuplicateFieldException(fieldName, resolveDuplicatePhoneMessage(fieldName));
                });

        entrepriseRepository.findByTelephone(telephone)
                .filter(existing -> currentEntrepriseId == null || !existing.getId().equals(currentEntrepriseId))
                .ifPresent(existing -> {
                    throw new DuplicateFieldException(fieldName, resolveDuplicatePhoneMessage(fieldName));
                });
    }

    private void validateMatriculeAvailable(String matricule, Long currentUserId) {
        if (matricule == null || matricule.isBlank()) {
            return;
        }

        utilisateurRepository.findByMatricule(matricule)
                .filter(existing -> currentUserId == null || !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new DuplicateFieldException("matricule", "Ce matricule est deja utilise.");
                });
    }

    private String resolveEmailMessage(String fieldName) {
        return switch (fieldName) {
            case "emailEntreprise" -> "L'email de l'entreprise doit contenir @.";
            case "emailResponsable" -> "L'email du responsable doit contenir @.";
            default -> "L'email doit contenir @.";
        };
    }

    private String resolvePhoneMessage(String fieldName) {
        return switch (fieldName) {
            case "telephoneEntreprise" -> "Le numero de telephone de l'entreprise est invalide.";
            case "telephoneResponsable" -> "Le numero de telephone du responsable est invalide.";
            default -> "Le numero de telephone est invalide.";
        };
    }

    private String resolveRequiredPhoneMessage(String fieldName) {
        return switch (fieldName) {
            case "telephoneEntreprise" -> "Le numero de telephone de l'entreprise est obligatoire.";
            case "telephoneResponsable" -> "Le numero de telephone du responsable est obligatoire.";
            default -> "Le numero de telephone est obligatoire.";
        };
    }

    private String resolveDuplicateEmailMessage(String fieldName) {
        return switch (fieldName) {
            case "emailEntreprise" -> "Cet email d'entreprise est deja utilise.";
            case "emailResponsable" -> "Cet email de responsable est deja utilise.";
            default -> "Cet email est deja utilise.";
        };
    }

    private String resolveDuplicatePhoneMessage(String fieldName) {
        return switch (fieldName) {
            case "telephoneEntreprise" -> "Ce numero de telephone d'entreprise est deja utilise.";
            case "telephoneResponsable" -> "Ce numero de telephone du responsable est deja utilise.";
            default -> "Ce numero de telephone est deja utilise.";
        };
    }
}
