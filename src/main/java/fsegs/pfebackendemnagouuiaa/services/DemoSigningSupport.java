package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.exception.BusinessException;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;

/**
 * Signatures de demonstration : evite les HTTP 400 lorsque le profil n'a pas encore d'image.
 */
public final class DemoSigningSupport {

    public static final String DEMO_SIGNATURE_DATA_URL =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

    private DemoSigningSupport() {
    }

    public static boolean isDemoAccountEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalized = email.trim().toLowerCase();
        return normalized.endsWith("@etudiant.tn")
                || normalized.endsWith("@fsegs.tn")
                || normalized.endsWith("@telnet.tn")
                || normalized.endsWith("@sofrecom.tn")
                || normalized.endsWith("@techcorp.tn")
                || normalized.contains("demo.");
    }

    /**
     * Retourne l'URL de signature du profil ; pour les comptes demo, applique et persiste un placeholder si absent.
     */
    public static String resolveSignatureUrl(Utilisateur utilisateur, UtilisateurRepository utilisateurRepository) {
        if (utilisateur == null || utilisateur.getId() == null) {
            throw new BusinessException("Utilisateur authentifie introuvable.");
        }

        String url = utilisateur.getUrlSignature();
        if (url != null && !url.isBlank()) {
            return url;
        }

        Utilisateur fresh = utilisateurRepository.findById(utilisateur.getId()).orElse(utilisateur);
        url = fresh.getUrlSignature();
        if (url != null && !url.isBlank()) {
            return url;
        }

        if (isDemoAccountEmail(fresh.getEmail())) {
            fresh.setUrlSignature(DEMO_SIGNATURE_DATA_URL);
            utilisateurRepository.save(fresh);
            return fresh.getUrlSignature();
        }

        throw new BusinessException("Veuillez enregistrer votre signature dans votre profil avant de continuer.");
    }
}
