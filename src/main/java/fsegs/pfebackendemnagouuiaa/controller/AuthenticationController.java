package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.AuthenticationRequest;
import fsegs.pfebackendemnagouuiaa.dto.AuthenticationResponse;
import fsegs.pfebackendemnagouuiaa.dto.ForgotPasswordRequest;
import fsegs.pfebackendemnagouuiaa.dto.PasswordResetResponse;
import fsegs.pfebackendemnagouuiaa.dto.ResetPasswordRequest;
import fsegs.pfebackendemnagouuiaa.dto.UserResponse;
import fsegs.pfebackendemnagouuiaa.services.AuthenticationService;
import fsegs.pfebackendemnagouuiaa.services.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST d'authentification et de récupération de session utilisateur.
 * <p>
 * <strong>Domaine exposé :</strong> connexion JWT, réinitialisation de mot de passe, profil courant.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/auth}
 * <p>
 * <strong>Sécurité :</strong> les endpoints {@code /login}, {@code /forgot-password} et
 * {@code /reset-password} sont publics ; {@code /me} exige un utilisateur authentifié
 * ({@code @PreAuthorize("isAuthenticated()")}).
 * <p>
 * <strong>Services injectés :</strong>
 * <ul>
 *   <li>{@link AuthenticationService} — authentification et flux mot de passe oublié</li>
 *   <li>{@link UtilisateurService} — lecture du profil de l'utilisateur connecté</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UtilisateurService utilisateurService;

    /**
     * Authentifie un utilisateur et retourne un jeton JWT.
     *
     * @param request identifiants de connexion (email, mot de passe)
     * @return {@link ResponseEntity} 200 avec {@link AuthenticationResponse} (token, rôles, etc.)
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         si les identifiants sont invalides (géré par le service / filtre de sécurité)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest request) {
        // Endpoint public : délégation complète au service d'authentification
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    /**
     * Initie la procédure de mot de passe oublié (envoi d'un lien ou code par e-mail).
     *
     * @param request adresse e-mail de l'utilisateur concerné
     * @return {@link ResponseEntity} 200 avec {@link PasswordResetResponse} (message de confirmation)
     * @throws jakarta.validation.ConstraintViolationException si la requête n'est pas valide
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authenticationService.forgotPassword(request));
    }

    /**
     * Réinitialise le mot de passe à partir d'un jeton ou code reçu par e-mail.
     *
     * @param request jeton de réinitialisation et nouveau mot de passe
     * @return {@link ResponseEntity} 200 avec {@link PasswordResetResponse}
     * @throws IllegalArgumentException si le jeton est expiré ou invalide (selon implémentation du service)
     */
    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authenticationService.resetPassword(request));
    }

    /**
     * Retourne le profil de l'utilisateur actuellement authentifié.
     *
     * @return {@link ResponseEntity} 200 avec {@link UserResponse}
     * @throws org.springframework.security.access.AccessDeniedException si non authentifié
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> me() {
        // @PreAuthorize garantit un contexte de sécurité valide avant l'appel service
        return ResponseEntity.ok(utilisateurService.getCurrentProfile());
    }
}
