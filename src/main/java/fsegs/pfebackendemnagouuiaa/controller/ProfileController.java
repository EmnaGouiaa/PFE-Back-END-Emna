package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.UpdatePasswordRequest;
import fsegs.pfebackendemnagouuiaa.dto.UserResponse;
import fsegs.pfebackendemnagouuiaa.services.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST simplifié pour la mise à jour du mot de passe du profil connecté.
 * <p>
 * <strong>Domaine exposé :</strong> gestion du mot de passe personnel.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/profile}
 * <p>
 * <strong>Sécurité :</strong> tous les endpoints exigent {@code isAuthenticated()}.
 * <p>
 * <strong>Services injectés :</strong> {@link UtilisateurService}
 * <p>
 * <strong>Note :</strong> des routes équivalentes existent sur {@link UtilisateurController}
 * ({@code PATCH /api/users/me/mot-de-passe}).
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UtilisateurService utilisateurService;

    /**
     * Met à jour le mot de passe de l'utilisateur actuellement connecté.
     *
     * @param request ancien et nouveau mot de passe (validés par Bean Validation)
     * @return {@link ResponseEntity} 200 avec {@link UserResponse} mis à jour
     * @throws org.springframework.security.access.AccessDeniedException si non authentifié
     * @throws IllegalArgumentException si l'ancien mot de passe est incorrect (selon le service)
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/password")
    public ResponseEntity<UserResponse> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        // @PreAuthorize : seul l'utilisateur connecté peut modifier son propre mot de passe
        return ResponseEntity.ok(utilisateurService.updateCurrentPassword(request));
    }
}
