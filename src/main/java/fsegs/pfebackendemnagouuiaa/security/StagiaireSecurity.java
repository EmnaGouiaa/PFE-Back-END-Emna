package fsegs.pfebackendemnagouuiaa.security;

import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import org.springframework.security.core.Authentication;

public class StagiaireSecurity {

    public static boolean estProprietaireOuAdmin(Utilisateur utilisateur, Long stagiaireId) {
        if (utilisateur == null || stagiaireId == null) {
            return false;
        }

        if (utilisateur.getRole() == Role.ADMINISTRATEUR) {
            return true;
        }

        if (utilisateur instanceof Stagiaire stagiaire) {
            return stagiaire.getId().equals(stagiaireId);
        }

        return false;
    }

    public static boolean peutAccederStagiaire(Authentication authentification, Long stagiaireId) {
        if (authentification == null || !authentification.isAuthenticated()) {
            return false;
        }

        Object principal = authentification.getPrincipal();
        if (!(principal instanceof Utilisateur utilisateur)) {
            return false;
        }

        return estProprietaireOuAdmin(utilisateur, stagiaireId);
    }
}