package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ChangeRoleRequest;
import fsegs.pfebackendemnagouuiaa.dto.CollaborateurSignatureDto;
import fsegs.pfebackendemnagouuiaa.dto.CreateUserRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateEmailRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateEmailResponse;
import fsegs.pfebackendemnagouuiaa.dto.UpdatePasswordRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateProfileRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateUserRequest;
import fsegs.pfebackendemnagouuiaa.dto.UserResponse;
import fsegs.pfebackendemnagouuiaa.services.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST de gestion des utilisateurs et de leurs profils.
 * <p>
 * <strong>Domaine exposé :</strong> comptes utilisateurs, profils, mots de passe, rôles, signatures.
 * <p>
 * <strong>Chemins de base :</strong> {@code /api/users} et alias {@code /api/utilisateurs}
 * <p>
 * <strong>Sécurité :</strong> {@code ADMINISTRATEUR} pour l'administration ;
 * {@code isAuthenticated()} pour le profil personnel ; {@code RESPONSABLE_ENTREPRISE} pour activation/désactivation.
 * <p>
 * <strong>Services injectés :</strong> {@link UtilisateurService}
 */
@RestController
@RequestMapping({"/api/users", "/api/utilisateurs"})
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    /**
     * Crée un nouvel utilisateur (réservé administrateur).
     *
     * @param request données de création validées
     * @return {@link ResponseEntity} 201 avec {@link UserResponse}
     */
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return new ResponseEntity<>(utilisateurService.createUser(request), HttpStatus.CREATED);
    }

    /**
     * Met à jour un utilisateur par identifiant (administration).
     *
     * @param id      identifiant utilisateur
     * @param request champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link UserResponse}
     */
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(utilisateurService.updateUser(id, request));
    }

    /**
     * Récupère la signature collaborateur d'un utilisateur (conventions, documents).
     *
     * @param id identifiant utilisateur
     * @return {@link ResponseEntity} 200 avec {@link CollaborateurSignatureDto}
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/signature-collaborateur")
    public ResponseEntity<CollaborateurSignatureDto> getCollaborateurSignature(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.getCollaborateurSignature(id));
    }

    /**
     * Récupère le profil public d'un utilisateur par identifiant.
     *
     * @param id identifiant utilisateur
     * @return {@link ResponseEntity} 200 avec {@link UserResponse}
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/profil")
    public ResponseEntity<UserResponse> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.getProfile(id));
    }

    /**
     * Récupère le profil de l'utilisateur actuellement connecté.
     *
     * @return {@link ResponseEntity} 200 avec {@link UserResponse}
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/profile")
    public ResponseEntity<UserResponse> getCurrentProfile() {
        return ResponseEntity.ok(utilisateurService.getCurrentProfile());
    }

    /**
     * Met à jour le profil d'un utilisateur (par identifiant).
     *
     * @param id      identifiant utilisateur
     * @param request champs de profil modifiables
     * @return {@link ResponseEntity} 200 avec {@link UserResponse}
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}/profil")
    public ResponseEntity<UserResponse> updateProfile(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(utilisateurService.updateProfile(id, request));
    }

    /**
     * Met à jour le profil de l'utilisateur connecté.
     *
     * @param request champs de profil modifiables
     * @return {@link ResponseEntity} 200 avec {@link UserResponse}
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/profile")
    public ResponseEntity<UserResponse> updateCurrentProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(utilisateurService.updateCurrentProfile(request));
    }

    /**
     * Met à jour l'e-mail de l'utilisateur connecté.
     *
     * @param request nouvelle adresse et confirmation
     * @return {@link ResponseEntity} 200 avec {@link UpdateEmailResponse}
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/email")
    public ResponseEntity<UpdateEmailResponse> updateCurrentEmail(@Valid @RequestBody UpdateEmailRequest request) {
        return ResponseEntity.ok(utilisateurService.updateCurrentEmail(request));
    }

    /**
     * Met à jour le mot de passe d'un utilisateur par identifiant.
     *
     * @param id      identifiant utilisateur
     * @param request ancien et nouveau mot de passe
     * @return {@link ResponseEntity} 200 avec {@link UserResponse}
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}/mot-de-passe")
    public ResponseEntity<UserResponse> updatePassword(@PathVariable Long id,
                                                       @Valid @RequestBody UpdatePasswordRequest request) {
        return ResponseEntity.ok(utilisateurService.updatePassword(id, request));
    }

    /**
     * Met à jour le mot de passe de l'utilisateur connecté.
     *
     * @param request ancien et nouveau mot de passe
     * @return {@link ResponseEntity} 200 avec {@link UserResponse}
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/mot-de-passe")
    public ResponseEntity<UserResponse> updateCurrentPassword(@Valid @RequestBody UpdatePasswordRequest request) {
        return ResponseEntity.ok(utilisateurService.updateCurrentPassword(request));
    }

    /**
     * Récupère un utilisateur par identifiant (administration).
     *
     * @param id identifiant utilisateur
     * @return {@link ResponseEntity} 200 avec {@link UserResponse}
     */
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.getUserById(id));
    }

    /**
     * Liste tous les utilisateurs (administration).
     *
     * @return {@link ResponseEntity} 200 avec la liste des {@link UserResponse}
     */
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(utilisateurService.getAllUsers());
    }

    /**
     * Désactive un compte utilisateur.
     * <p>Accessible à l'administrateur ou au responsable entreprise (collaborateurs de son entreprise).
     *
     * @param id identifiant utilisateur
     * @return {@link ResponseEntity} 200 avec {@link UserResponse} désactivé
     */
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_ENTREPRISE')")
    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.deactivateUser(id));
    }

    /**
     * Réactive un compte utilisateur désactivé.
     *
     * @param id identifiant utilisateur
     * @return {@link ResponseEntity} 200 avec {@link UserResponse} activé
     */
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_ENTREPRISE')")
    @PatchMapping("/{id}/activer")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.activateUser(id));
    }

    /**
     * Supprime l'image de signature d'un utilisateur (administration).
     *
     * @param id identifiant utilisateur
     * @return {@link ResponseEntity} 200 avec {@link UserResponse}
     */
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @DeleteMapping("/{id}/signature")
    public ResponseEntity<UserResponse> deleteUserSignature(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.deleteUserSignature(id));
    }

    /**
     * Supprime définitivement un utilisateur (administration).
     *
     * @param id identifiant utilisateur
     * @return {@link ResponseEntity} 204 sans corps
     */
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        utilisateurService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Modification de rôle désactivée (règle métier : rôles fixes après création du compte).
     *
     * @return {@link ResponseEntity} 403 via {@link org.springframework.security.access.AccessDeniedException}
     */
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeUserRole(@PathVariable Long id,
                                                       @Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(utilisateurService.changeUserRole(id, request));
    }
}
