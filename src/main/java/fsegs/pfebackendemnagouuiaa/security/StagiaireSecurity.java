package fsegs.pfebackendemnagouuiaa.security;

import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import org.springframework.security.core.Authentication;

/**
 * Utilitaires de contrôle d'accès orientés « ressource stagiaire ».
 *
 * <p><b>Rôle :</b> centraliser la logique « un stagiaire ne peut accéder qu'à ses propres
 * données ; un administrateur peut tout consulter » pour les expressions
 * {@code @PreAuthorize} et les vérifications manuelles dans les services.</p>
 *
 * <p><b>Responsabilités :</b></p>
 * <ul>
 *   <li>Vérifier la propriété d'un enregistrement stagiaire par identifiant.</li>
 *   <li>Adapter le principal {@link Authentication} vers {@link Utilisateur}.</li>
 * </ul>
 *
 * <p><b>Relations :</b> utilisé par les contrôleurs et services stagiaire ; s'appuie sur
 * l'héritage JPA {@link Stagiaire} extends {@link Utilisateur} et le rôle
 * {@link Role#ADMINISTRATEUR}.</p>
 */
public class StagiaireSecurity {

    /**
     * Détermine si l'utilisateur connecté peut accéder aux données du stagiaire ciblé.
     *
     * <p><b>Règles métier :</b></p>
     * <ul>
     *   <li>{@code null} utilisateur ou identifiant → refus.</li>
     *   <li>Rôle {@link Role#ADMINISTRATEUR} → accès autorisé.</li>
     *   <li>Principal de type {@link Stagiaire} avec le même {@code id} → accès autorisé.</li>
     *   <li>Autres rôles → refus.</li>
     * </ul>
     *
     * @param utilisateur  principal authentifié (peut être {@code null})
     * @param stagiaireId  identifiant de la ressource stagiaire demandée
     * @return {@code true} si l'accès est permis
     */
    public static boolean estProprietaireOuAdmin(Utilisateur utilisateur, Long stagiaireId) {
        if (utilisateur == null || stagiaireId == null) {
            return false;
        }

        // Bypass administratif : accès transversal à tous les dossiers stagiaires
        if (utilisateur.getRole() == Role.ADMINISTRATEUR) {
            return true;
        }

        // Propriété : le stagiaire connecté ne peut cibler que son propre identifiant
        if (utilisateur instanceof Stagiaire stagiaire) {
            return stagiaire.getId().equals(stagiaireId);
        }

        return false;
    }

    /**
     * Variante utilisant directement l'objet {@link Authentication} Spring Security.
     *
     * @param authentification contexte de sécurité de la requête courante
     * @param stagiaireId    identifiant de la ressource stagiaire
     * @return {@code true} si authentifié et {@link #estProprietaireOuAdmin} retourne vrai
     */
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
