package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.exception.DuplicateFieldException;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UtilisateurUniciteService {

    private final UtilisateurRepository utilisateurRepository;

    public String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        String normalized = email.trim().toLowerCase();
        return normalized.isEmpty() ? null : normalized;
    }

    public String normalizeTelephone(String telephone) {
        if (telephone == null) {
            return null;
        }

        String normalized = telephone.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public void validateForCreate(String email, String telephone) {
        validateEmailAvailable(email, null);
        validateTelephoneAvailable(telephone, null);
    }

    public void validateForUpdate(Long userId, String email, String telephone) {
        validateEmailAvailable(email, userId);
        validateTelephoneAvailable(telephone, userId);
    }

    private void validateEmailAvailable(String email, Long currentUserId) {
        String normalized = normalizeEmail(email);
        if (normalized == null) {
            return;
        }

        utilisateurRepository.findByEmailIgnoreCase(normalized)
                .filter(existing -> currentUserId == null || !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new DuplicateFieldException("Cet email est deja utilise par un autre utilisateur.");
                });
    }

    private void validateTelephoneAvailable(String telephone, Long currentUserId) {
        String normalized = normalizeTelephone(telephone);
        if (normalized == null) {
            return;
        }

        utilisateurRepository.findByTelephone(normalized)
                .filter(existing -> currentUserId == null || !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new DuplicateFieldException("Ce numero de telephone est deja utilise par un autre utilisateur.");
                });
    }
}
